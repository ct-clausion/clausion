package com.classpulse.domain.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<AttendanceRecord, Long> {
    List<AttendanceRecord> findBySessionId(Long sessionId);
    List<AttendanceRecord> findByStudentId(Long studentId);

    @Query("SELECT COUNT(a) FROM AttendanceRecord a WHERE a.sessionId = :sessionId AND a.status = :status")
    long countBySessionIdAndStatus(@Param("sessionId") Long sessionId, @Param("status") String status);

    @Query("SELECT COUNT(a) FROM AttendanceRecord a WHERE a.studentId = :studentId AND a.status = 'PRESENT'")
    long countPresentByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT COUNT(a) FROM AttendanceRecord a WHERE a.studentId = :studentId")
    long countTotalByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT COUNT(a) FROM AttendanceRecord a WHERE a.studentId = :studentId AND a.status = 'PRESENT' AND a.sessionId IN (SELECT s.id FROM CourseSession s WHERE s.courseId = :courseId)")
    long countPresentByStudentIdAndCourseId(@Param("studentId") Long studentId, @Param("courseId") Long courseId);

    @Query("SELECT COUNT(a) FROM AttendanceRecord a WHERE a.studentId = :studentId AND a.sessionId IN (SELECT s.id FROM CourseSession s WHERE s.courseId = :courseId)")
    long countTotalByStudentIdAndCourseId(@Param("studentId") Long studentId, @Param("courseId") Long courseId);
}
