package com.classpulse.domain.gamification;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GamificationRepository extends JpaRepository<StudentGamification, Long> {
    Optional<StudentGamification> findByStudentIdAndCourseId(Long studentId, Long courseId);
    List<StudentGamification> findByStudentId(Long studentId);

    // Leaderboard endpoint dereferences student.name outside a transaction, so
    // eager-fetch the student to avoid LazyInit on non-empty courses.
    @EntityGraph(attributePaths = "student")
    List<StudentGamification> findByCourseIdOrderByTotalXpEarnedDesc(Long courseId);
}
