package com.classpulse.domain.course;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CurriculumSkillRepository extends JpaRepository<CurriculumSkill, Long> {

    @Query("SELECT DISTINCT s FROM CurriculumSkill s LEFT JOIN FETCH s.prerequisites WHERE s.course.id = :courseId")
    List<CurriculumSkill> findByCourseId(@Param("courseId") Long courseId);

    @Modifying
    @Query(value = "DELETE FROM skill_prerequisites WHERE skill_id = :skillId OR prerequisite_skill_id = :skillId", nativeQuery = true)
    void deletePrerequisiteLinks(@Param("skillId") Long skillId);

    @Modifying
    @Query(value = "UPDATE questions SET skill_id = NULL WHERE skill_id = :skillId", nativeQuery = true)
    void nullifyQuestionSkillLinks(@Param("skillId") Long skillId);

    @Modifying
    @Query(value = "UPDATE code_feedbacks SET twin_skill_id = NULL WHERE twin_skill_id = :skillId", nativeQuery = true)
    void nullifyCodeFeedbackSkillLinks(@Param("skillId") Long skillId);

    @Modifying
    @Query(value = "UPDATE review_tasks SET skill_id = NULL WHERE skill_id = :skillId", nativeQuery = true)
    void nullifyReviewTaskSkillLinks(@Param("skillId") Long skillId);

    @Modifying
    @Query(value = "UPDATE code_submissions SET skill_id = NULL WHERE skill_id = :skillId", nativeQuery = true)
    void nullifyCodeSubmissionSkillLinks(@Param("skillId") Long skillId);

    @Modifying
    @Query(value = "UPDATE action_plans SET linked_skill_id = NULL WHERE linked_skill_id = :skillId", nativeQuery = true)
    void nullifyActionPlanSkillLinks(@Param("skillId") Long skillId);
}
