package com.classpulse.domain.twin;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentTwinRepository extends JpaRepository<StudentTwin, Long> {
    Optional<StudentTwin> findByStudentIdAndCourseId(Long studentId, Long courseId);
    List<StudentTwin> findByStudentId(Long studentId);

    // Every caller eventually reads student.name/email outside a transaction
    // (instructor students list, operator personnel, study-group matcher).
    // Eager-fetch the student to avoid LazyInit across the board.
    @EntityGraph(attributePaths = "student")
    List<StudentTwin> findByCourseId(Long courseId);

    List<StudentTwin> findByCourseIdAndOverallRiskScoreGreaterThan(Long courseId, java.math.BigDecimal threshold);
}
