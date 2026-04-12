package com.classpulse.api;

import com.classpulse.config.SecurityUtil;
import com.classpulse.domain.attendance.*;
import com.classpulse.domain.audit.OperatorAuditLog;
import com.classpulse.domain.audit.AuditLogRepository;
import com.classpulse.domain.course.CourseEnrollment;
import com.classpulse.domain.course.CourseEnrollmentRepository;
import com.classpulse.domain.user.User;
import com.classpulse.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/operator/attendance")
@RequiredArgsConstructor
@Transactional
public class AttendanceController {

    private final CourseSessionRepository courseSessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    @GetMapping("/sessions/{courseId}")
    public ResponseEntity<List<Map<String, Object>>> getSessions(@PathVariable Long courseId) {
        List<CourseSession> sessions = courseSessionRepository.findByCourseIdOrderBySessionDateDesc(courseId);
        List<Map<String, Object>> result = sessions.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("courseId", s.getCourseId());
            m.put("sessionDate", s.getSessionDate().toString());
            m.put("sessionNumber", s.getSessionNumber());
            m.put("title", s.getTitle());
            m.put("status", s.getStatus());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/sessions")
    public ResponseEntity<Map<String, Object>> createSession(@RequestBody Map<String, Object> body) {
        Long courseId = Long.valueOf(body.get("courseId").toString());
        LocalDate date = LocalDate.parse((String) body.get("sessionDate"));
        String title = (String) body.getOrDefault("title", null);

        int nextNum = courseSessionRepository.findMaxSessionNumber(courseId, date) + 1;
        CourseSession session = CourseSession.builder()
                .courseId(courseId)
                .sessionDate(date)
                .sessionNumber(nextNum)
                .title(title)
                .build();
        courseSessionRepository.save(session);

        // Auto-create attendance records for all enrolled students
        List<CourseEnrollment> enrollments = courseEnrollmentRepository.findByCourseId(courseId);
        for (CourseEnrollment enrollment : enrollments) {
            AttendanceRecord record = AttendanceRecord.builder()
                    .sessionId(session.getId())
                    .studentId(enrollment.getStudent().getId())
                    .status("ABSENT")
                    .recordedBy(SecurityUtil.getCurrentUserId())
                    .build();
            attendanceRepository.save(record);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", session.getId());
        result.put("courseId", session.getCourseId());
        result.put("sessionDate", session.getSessionDate().toString());
        result.put("sessionNumber", session.getSessionNumber());
        result.put("title", session.getTitle());
        result.put("status", session.getStatus());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/records")
    public ResponseEntity<List<Map<String, Object>>> getAttendanceRecords(@RequestParam Long sessionId) {
        List<AttendanceRecord> records = attendanceRepository.findBySessionId(sessionId);
        List<Map<String, Object>> result = records.stream().map(ar -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", ar.getId());
            m.put("sessionId", ar.getSessionId());
            m.put("studentId", ar.getStudentId());
            User student = userRepository.findById(ar.getStudentId()).orElse(null);
            m.put("studentName", student != null ? student.getName() : "Unknown");
            m.put("status", ar.getStatus());
            m.put("checkInTime", ar.getCheckInTime() != null ? ar.getCheckInTime().toString() : null);
            m.put("note", ar.getNote());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/records")
    public ResponseEntity<Void> bulkUpdateAttendance(@RequestBody List<Map<String, String>> records) {
        Long operatorId = SecurityUtil.getCurrentUserId();
        for (Map<String, String> rec : records) {
            Long sessionId = Long.valueOf(rec.get("sessionId"));
            Long studentId = Long.valueOf(rec.get("studentId"));
            String status = rec.get("status");

            List<AttendanceRecord> existing = attendanceRepository.findBySessionId(sessionId).stream()
                    .filter(a -> a.getStudentId().equals(studentId))
                    .collect(Collectors.toList());

            if (!existing.isEmpty()) {
                AttendanceRecord ar = existing.get(0);
                ar.setStatus(status);
                ar.setRecordedBy(operatorId);
                if ("PRESENT".equals(status) || "LATE".equals(status)) {
                    ar.setCheckInTime(java.time.LocalDateTime.now());
                }
                attendanceRepository.save(ar);
            } else {
                AttendanceRecord ar = AttendanceRecord.builder()
                        .sessionId(sessionId)
                        .studentId(studentId)
                        .status(status)
                        .recordedBy(operatorId)
                        .build();
                if ("PRESENT".equals(status) || "LATE".equals(status)) {
                    ar.setCheckInTime(java.time.LocalDateTime.now());
                }
                attendanceRepository.save(ar);
            }
        }
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/records/{id}")
    public ResponseEntity<Void> updateAttendance(@PathVariable Long id, @RequestBody Map<String, String> body) {
        AttendanceRecord ar = attendanceRepository.findById(id).orElse(null);
        if (ar == null) return ResponseEntity.notFound().build();

        ar.setStatus(body.get("status"));
        ar.setNote(body.get("note"));
        ar.setRecordedBy(SecurityUtil.getCurrentUserId());
        attendanceRepository.save(ar);

        try {
            OperatorAuditLog log = OperatorAuditLog.builder()
                    .operatorId(SecurityUtil.getCurrentUserId())
                    .actionType("ATTENDANCE_EDIT")
                    .targetType("ATTENDANCE")
                    .targetId(id)
                    .details(Map.of())
                    .build();
            auditLogRepository.save(log);
        } catch (Exception e) {
            log.error("Failed to write audit log", e);
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats/{courseId}")
    public ResponseEntity<Map<String, Object>> getAttendanceStats(@PathVariable Long courseId) {
        List<CourseSession> sessions = courseSessionRepository.findByCourseIdOrderBySessionDateDesc(courseId);
        List<CourseEnrollment> enrollments = courseEnrollmentRepository.findByCourseId(courseId);

        long totalPresent = 0;
        long totalRecords = 0;

        List<Map<String, Object>> studentStats = new ArrayList<>();
        for (CourseEnrollment enrollment : enrollments) {
            Long studentId = enrollment.getStudent().getId();
            long present = attendanceRepository.countPresentByStudentIdAndCourseId(studentId, courseId);
            long total = attendanceRepository.countTotalByStudentIdAndCourseId(studentId, courseId);
            totalPresent += present;
            totalRecords += total;

            User student = userRepository.findById(studentId).orElse(null);
            Map<String, Object> ss = new LinkedHashMap<>();
            ss.put("studentId", studentId);
            ss.put("studentName", student != null ? student.getName() : "Unknown");
            ss.put("presentCount", present);
            ss.put("totalCount", total);
            ss.put("rate", total > 0 ? (double) present / total : 0.0);
            studentStats.add(ss);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("courseId", courseId);
        result.put("totalSessions", sessions.size());
        result.put("avgAttendanceRate", totalRecords > 0 ? (double) totalPresent / totalRecords : 0.0);
        result.put("studentStats", studentStats);
        return ResponseEntity.ok(result);
    }
}
