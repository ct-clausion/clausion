package com.classpulse.domain.twin;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SkillMasterySnapshotRepository extends JpaRepository<SkillMasterySnapshot, Long> {
    List<SkillMasterySnapshot> findByStudentIdAndSkillIdOrderByCapturedAtDesc(Long studentId, Long skillId);

    // TwinController.getHistory serializes skill.id and skill.name outside a
    // transaction — plain finders hit LazyInit. @EntityGraph makes the skill
    // eager-loaded for these two list calls without changing the method names
    // (Spring Data still parses them for the WHERE / ORDER BY / LIMIT 10).
    @EntityGraph(attributePaths = "skill")
    List<SkillMasterySnapshot> findByStudentIdAndCourseIdOrderByCapturedAtDesc(Long studentId, Long courseId);

    @EntityGraph(attributePaths = "skill")
    List<SkillMasterySnapshot> findTop10ByStudentIdOrderByCapturedAtDesc(Long studentId);

    void deleteBySkillId(Long skillId);
}
