package com.classpulse.domain.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<OperatorAuditLog, Long> {
    Page<OperatorAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<OperatorAuditLog> findByOperatorIdOrderByCreatedAtDesc(Long operatorId, Pageable pageable);
}
