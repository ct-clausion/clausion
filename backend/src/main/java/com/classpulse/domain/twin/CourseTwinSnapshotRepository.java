package com.classpulse.domain.twin;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CourseTwinSnapshotRepository extends JpaRepository<CourseTwinSnapshot, Long> {
    Optional<CourseTwinSnapshot> findByCourseIdAndSnapshotDate(Long courseId, LocalDate date);
    List<CourseTwinSnapshot> findByCourseIdOrderBySnapshotDateDesc(Long courseId);
    List<CourseTwinSnapshot> findBySnapshotDate(LocalDate date);
}
