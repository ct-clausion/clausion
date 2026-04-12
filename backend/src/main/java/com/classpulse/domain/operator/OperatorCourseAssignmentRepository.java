package com.classpulse.domain.operator;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OperatorCourseAssignmentRepository extends JpaRepository<OperatorCourseAssignment, Long> {
    List<OperatorCourseAssignment> findByOperatorId(Long operatorId);
    List<OperatorCourseAssignment> findByCourseId(Long courseId);
}
