package com.classpulse.domain.course;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    @EntityGraph(attributePaths = {"weeks", "createdBy"})
    List<Course> findByCreatedByIdAndStatus(Long instructorId, String status);

    List<Course> findByCreatedById(Long instructorId);

    @EntityGraph(attributePaths = {"weeks", "createdBy"})
    List<Course> findByStatus(String status);

    @EntityGraph(attributePaths = {"weeks", "createdBy"})
    List<Course> findByStatusAndApprovalStatus(String status, String approvalStatus);

    @EntityGraph(attributePaths = {"weeks", "createdBy"})
    Optional<Course> findById(Long id);

    /** Row-level lock for enrollment capacity checks. Prevents two concurrent
     *  enrollments from both passing the count-vs-capacity gate. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Course c WHERE c.id = :id")
    Optional<Course> findByIdForUpdate(@Param("id") Long id);
}
