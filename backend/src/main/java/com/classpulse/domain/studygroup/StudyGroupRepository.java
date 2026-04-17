package com.classpulse.domain.studygroup;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface StudyGroupRepository extends JpaRepository<StudyGroup, Long> {
    @EntityGraph(attributePaths = {"members", "members.student", "course", "createdBy"})
    List<StudyGroup> findByCourseId(Long courseId);

    @EntityGraph(attributePaths = {"members", "members.student", "course", "createdBy"})
    @Query("SELECT sg FROM StudyGroup sg JOIN sg.members m WHERE m.student.id = :studentId")
    List<StudyGroup> findByMemberStudentId(Long studentId);

    @EntityGraph(attributePaths = {"members", "members.student", "course", "createdBy"})
    Optional<StudyGroup> findById(Long id);

    /** Row-level write lock for join/leave flows — prevents concurrent capacity overruns. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT sg FROM StudyGroup sg WHERE sg.id = :id")
    Optional<StudyGroup> findByIdForUpdate(@Param("id") Long id);
}
