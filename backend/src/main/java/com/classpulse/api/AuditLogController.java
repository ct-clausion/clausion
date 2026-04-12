package com.classpulse.api;

import com.classpulse.domain.audit.AuditLogRepository;
import com.classpulse.domain.audit.OperatorAuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/operator/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<OperatorAuditLog> logPage = auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));

        List<Map<String, Object>> content = logPage.getContent().stream().map(log -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", log.getId());
            m.put("operatorId", log.getOperatorId());
            m.put("actionType", log.getActionType());
            m.put("targetType", log.getTargetType());
            m.put("targetId", log.getTargetId());
            m.put("details", log.getDetails());
            m.put("createdAt", log.getCreatedAt() != null ? log.getCreatedAt().toString() : null);
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("content", content);
        result.put("totalPages", logPage.getTotalPages());
        result.put("totalElements", logPage.getTotalElements());
        return ResponseEntity.ok(result);
    }
}
