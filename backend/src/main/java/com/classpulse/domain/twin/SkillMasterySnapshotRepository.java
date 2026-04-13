package com.classpulse.domain.twin;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SkillMasterySnapshotRepository extends JpaRepository<SkillMasterySnapshot, Long> {
    List<SkillMasterySnapshot> findByStudentIdAndSkillIdOrderByCapturedAtDesc(Long studentId, Long skillId);
    List<SkillMasterySnapshot> findByStudentIdAndCourseIdOrderByCapturedAtDesc(Long studentId, Long courseId);
    List<SkillMasterySnapshot> findTop10ByStudentIdOrderByCapturedAtDesc(Long studentId);
    void deleteBySkillId(Long skillId);
}
