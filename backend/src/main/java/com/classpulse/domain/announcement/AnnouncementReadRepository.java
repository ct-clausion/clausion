package com.classpulse.domain.announcement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

public interface AnnouncementReadRepository extends JpaRepository<AnnouncementRead, Long> {
    long countByAnnouncementId(Long announcementId);
    Optional<AnnouncementRead> findByAnnouncementIdAndUserId(Long announcementId, Long userId);
    @Transactional
    void deleteByAnnouncementId(Long announcementId);
}
