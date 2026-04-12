package com.classpulse.domain.operator;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OperatorProfileRepository extends JpaRepository<OperatorProfile, Long> {
    Optional<OperatorProfile> findByUserId(Long userId);
}
