package com.classpulse.api;

import com.classpulse.config.SecurityUtil;
import com.classpulse.domain.attendance.*;
import com.classpulse.domain.course.Course;
import com.classpulse.domain.course.CourseEnrollment;
import com.classpulse.domain.course.CourseEnrollmentRepository;
import com.classpulse.domain.course.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 학생 출결 API.
 * - 오늘 출석 체크인
 * - 내 출결 현황 조회
 */
@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudentAttendanceController {

    private final CourseEnrollmentRepository courseEnrollmentRepository;
    private final CourseSessionRepository courseSessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final CourseRepository courseRepository;

    /**
     * 오늘 내가 수강 중인 과정의 세션 + 출석 상태 조회.
     * 학생이 출석 버튼을 누를 수 있는 세션 목록을 반환.
     */
    @GetMapping("/today")
    public ResponseEntity<List<Map<String, Object>>> getTodaySessions() {
        Long studentId = SecurityUtil.getCurrentUserId();
        LocalDate today = LocalDate.now();

        // 수강 중인 과정 ID 목록
        List<CourseEnrollment> enrollments = courseEnrollmentRepository.findByStudentIdAndStatus(studentId, "ACTIVE");
        if (enrollments.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (CourseEnrollment enrollment : enrollments) {
            Long courseId = enrollment.getCourse().getId();
            Course course = courseRepository.findById(courseId).orElse(null);
            if (course == null) continue;

            // 오늘 날짜의 세션 조회
            List<CourseSession> todaySessions = courseSessionRepository
                    .findByCourseIdAndSessionDateBetween(courseId, today, today);

            for (CourseSession session : todaySessions) {
                // 이 학생의 출결 레코드 조회
                AttendanceRecord record = attendanceRepository.findBySessionId(session.getId())
                        .stream()
                        .filter(a -> a.getStudentId().equals(studentId))
                        .findFirst()
                        .orElse(null);

                Map<String, Object> m = new LinkedHashMap<>();
                m.put("sessionId", session.getId());
                m.put("courseId", courseId);
                m.put("courseTitle", course.getTitle());
                m.put("sessionDate", session.getSessionDate().toString());
                m.put("sessionTitle", session.getTitle());
                m.put("sessionNumber", session.getSessionNumber());
                m.put("status", record != null ? record.getStatus() : "NO_RECORD");
                m.put("checkInTime", record != null && record.getCheckInTime() != null
                        ? record.getCheckInTime().toString() : null);
                m.put("canCheckIn", record != null && "ABSENT".equals(record.getStatus()));
                result.add(m);
            }
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 학생 출석 체크인.
     * 본인의 ABSENT 상태 레코드를 PRESENT로 변경.
     */
    @PostMapping("/check-in")
    @Transactional
    public ResponseEntity<Map<String, Object>> checkIn(@RequestBody Map<String, Object> body) {
        Long studentId = SecurityUtil.getCurrentUserId();
        Long sessionId = Long.valueOf(body.get("sessionId").toString());

        // PESSIMISTIC_WRITE lock — concurrent double-clicks can't both pass the status
        // check and double-mark PRESENT.
        AttendanceRecord record = attendanceRepository
                .findBySessionIdAndStudentIdForUpdate(sessionId, studentId)
                .orElse(null);

        if (record == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "출결 레코드가 없습니다. 세션에 등록되지 않았습니다."));
        }

        if ("PRESENT".equals(record.getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("error", "이미 출석 처리되었습니다."));
        }

        record.setStatus("PRESENT");
        record.setCheckInTime(LocalDateTime.now());
        record.setRecordedBy(studentId);
        attendanceRepository.save(record);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", sessionId);
        result.put("status", "PRESENT");
        result.put("checkInTime", record.getCheckInTime().toString());
        result.put("message", "출석 처리되었습니다.");
        return ResponseEntity.ok(result);
    }

    /**
     * 내 출결 현황 조회 (과정별 통계).
     */
    @GetMapping("/my")
    public ResponseEntity<List<Map<String, Object>>> getMyAttendance() {
        Long studentId = SecurityUtil.getCurrentUserId();
        List<CourseEnrollment> enrollments = courseEnrollmentRepository.findByStudentIdAndStatus(studentId, "ACTIVE");

        List<Map<String, Object>> result = new ArrayList<>();
        for (CourseEnrollment enrollment : enrollments) {
            Long courseId = enrollment.getCourse().getId();
            Course course = courseRepository.findById(courseId).orElse(null);
            if (course == null) continue;

            long present = attendanceRepository.countPresentByStudentIdAndCourseId(studentId, courseId);
            long total = attendanceRepository.countTotalByStudentIdAndCourseId(studentId, courseId);
            double rate = total > 0 ? (double) present / total : 0.0;

            // 최근 출결 기록 (최신 5개)
            List<CourseSession> sessions = courseSessionRepository.findByCourseIdOrderBySessionDateDesc(courseId);
            List<Map<String, Object>> recentRecords = new ArrayList<>();
            int count = 0;
            for (CourseSession session : sessions) {
                if (count >= 5) break;
                AttendanceRecord rec = attendanceRepository.findBySessionId(session.getId())
                        .stream()
                        .filter(a -> a.getStudentId().equals(studentId))
                        .findFirst()
                        .orElse(null);
                if (rec != null) {
                    Map<String, Object> rm = new LinkedHashMap<>();
                    rm.put("sessionDate", session.getSessionDate().toString());
                    rm.put("sessionTitle", session.getTitle());
                    rm.put("status", rec.getStatus());
                    recentRecords.add(rm);
                    count++;
                }
            }

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("courseId", courseId);
            m.put("courseTitle", course.getTitle());
            m.put("presentCount", present);
            m.put("totalCount", total);
            m.put("attendanceRate", rate);
            m.put("recentRecords", recentRecords);
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }
}
