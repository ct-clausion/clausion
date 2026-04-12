package com.classpulse.domain.intervention;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface InterventionLogRepository extends JpaRepository<InterventionLog, Long> {
    List<InterventionLog> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<InterventionLog> findByCourseIdOrderByCreatedAtDesc(Long courseId);
    List<InterventionLog> findByStatusOrderByCreatedAtDesc(String status);
}
