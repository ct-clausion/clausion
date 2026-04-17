package com.classpulse.domain.attendance;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<AttendanceRecord, Long> {
    List<AttendanceRecord> findBySessionId(Long sessionId);
    List<AttendanceRecord> findByStudentId(Long studentId);

    Optional<AttendanceRecord> findBySessionIdAndStudentId(Long sessionId, Long studentId);

    /** Locks the row for the duration of the tx. Use for check-in to prevent double-writes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AttendanceRecord a WHERE a.sessionId = :sessionId AND a.studentId = :studentId")
    Optional<AttendanceRecord> findBySessionIdAndStudentIdForUpdate(
            @Param("sessionId") Long sessionId, @Param("studentId") Long studentId);

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
