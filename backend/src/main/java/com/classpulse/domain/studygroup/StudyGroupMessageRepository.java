package com.classpulse.domain.studygroup;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyGroupMessageRepository extends JpaRepository<StudyGroupMessage, Long> {

    @EntityGraph(attributePaths = {"sender"})
    List<StudyGroupMessage> findByStudyGroupIdOrderByCreatedAtDesc(Long studyGroupId, Pageable pageable);

    void deleteByStudyGroupId(Long studyGroupId);
}
