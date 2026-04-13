package com.classpulse.api;

import com.classpulse.config.SecurityUtil;
import com.classpulse.domain.attendance.*;
import com.classpulse.domain.course.Course;
import com.classpulse.domain.course.CourseEnrollment;
import com.classpulse.domain.course.CourseEnrollmentRepository;
import com.classpulse.domain.course.CourseRepository;
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

/**
 * 강사 전용 출결 관리 API.
 * /api/instructor/** 경로 → hasRole("INSTRUCTOR") 보안 적용.
 * 강사 본인이 생성한 과정만 접근 가능.
 */
@Slf4j
@RestController
@RequestMapping("/api/instructor/attendance")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InstructorAttendanceController {

    private final CourseRepository courseRepository;
    private final CourseSessionRepository courseSessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final UserRepository userRepository;

    /** 강사 본인의 과정 목록 */
    @GetMapping("/courses")
    public ResponseEntity<List<Map<String, Object>>> getMyCourses() {
        Long instructorId = SecurityUtil.getCurrentUserId();
        List<Course> courses = courseRepository.findByCreatedById(instructorId);
        List<Map<String, Object>> result = courses.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("title", c.getTitle());
            m.put("status", c.getStatus());
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    /** 과정의 세션 목록 (강사 본인 과정만) */
    @GetMapping("/sessions/{courseId}")
    public ResponseEntity<List<Map<String, Object>>> getSessions(@PathVariable Long courseId) {
        if (!isOwnCourse(courseId)) {
            return ResponseEntity.status(403).build();
        }
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

    /** 세션 생성 */
    @PostMapping("/sessions")
    @Transactional
    public ResponseEntity<Map<String, Object>> createSession(@RequestBody Map<String, Object> body) {
        Long courseId = Long.valueOf(body.get("courseId").toString());
        if (!isOwnCourse(courseId)) {
            return ResponseEntity.status(403).build();
        }

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

        // 수강생 전원에게 출결 레코드 자동 생성 (기본: 결석)
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

    /** 세션별 출결 기록 조회 */
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

    /** 출결 일괄 업데이트 */
    @PostMapping("/records")
    @Transactional
    public ResponseEntity<Void> bulkUpdateAttendance(@RequestBody List<Map<String, String>> records) {
        Long instructorId = SecurityUtil.getCurrentUserId();
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
                ar.setRecordedBy(instructorId);
                if ("PRESENT".equals(status) || "LATE".equals(status)) {
                    ar.setCheckInTime(java.time.LocalDateTime.now());
                }
                attendanceRepository.save(ar);
            } else {
                AttendanceRecord ar = AttendanceRecord.builder()
                        .sessionId(sessionId)
                        .studentId(studentId)
                        .status(status)
                        .recordedBy(instructorId)
                        .build();
                if ("PRESENT".equals(status) || "LATE".equals(status)) {
                    ar.setCheckInTime(java.time.LocalDateTime.now());
                }
                attendanceRepository.save(ar);
            }
        }
        return ResponseEntity.noContent().build();
    }

    /** 과정별 출결 통계 */
    @GetMapping("/stats/{courseId}")
    public ResponseEntity<Map<String, Object>> getAttendanceStats(@PathVariable Long courseId) {
        if (!isOwnCourse(courseId)) {
            return ResponseEntity.status(403).build();
        }

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

    /** 강사 본인이 생성한 과정인지 확인 */
    private boolean isOwnCourse(Long courseId) {
        Long instructorId = SecurityUtil.getCurrentUserId();
        return courseRepository.findById(courseId)
                .map(c -> c.getCreatedBy() != null && c.getCreatedBy().getId().equals(instructorId))
                .orElse(false);
    }
}
