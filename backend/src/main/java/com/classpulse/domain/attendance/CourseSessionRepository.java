package com.classpulse.domain.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface CourseSessionRepository extends JpaRepository<CourseSession, Long> {
    List<CourseSession> findByCourseIdOrderBySessionDateDesc(Long courseId);
    List<CourseSession> findBySessionDate(LocalDate date);
    List<CourseSession> findByCourseIdAndSessionDateBetween(Long courseId, LocalDate start, LocalDate end);

    @Query("SELECT COALESCE(MAX(s.sessionNumber), 0) FROM CourseSession s WHERE s.courseId = :courseId AND s.sessionDate = :date")
    int findMaxSessionNumber(@Param("courseId") Long courseId, @Param("date") LocalDate date);
}
