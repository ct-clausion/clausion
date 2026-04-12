package com.classpulse.api;

import com.classpulse.config.SecurityUtil;
import com.classpulse.domain.audit.OperatorAuditLog;
import com.classpulse.domain.audit.AuditLogRepository;
import com.classpulse.domain.course.Course;
import com.classpulse.domain.course.CourseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/operator/courses")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OperatorCourseController {

    private final CourseRepository courseRepository;
    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getCourses() {
        List<Course> courses = courseRepository.findAll();
        List<Map<String, Object>> result = courses.stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("title", c.getTitle());
            m.put("description", c.getDescription());
            m.put("status", c.getStatus());
            m.put("approvalStatus", c.getApprovalStatus() != null ? c.getApprovalStatus() : "APPROVED");
            m.put("approvalNote", c.getApprovalNote());
            m.put("maxCapacity", c.getMaxCapacity() != null ? c.getMaxCapacity() : 30);
            m.put("schedule", c.getSchedule());
            m.put("createdBy", c.getCreatedBy() != null ? c.getCreatedBy().getName() : null);
            m.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
            return m;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getCourse(@PathVariable Long id) {
        Course c = courseRepository.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("title", c.getTitle());
        m.put("description", c.getDescription());
        m.put("status", c.getStatus());
        m.put("approvalStatus", c.getApprovalStatus());
        m.put("approvalNote", c.getApprovalNote());
        m.put("maxCapacity", c.getMaxCapacity());
        m.put("createdAt", c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
        return ResponseEntity.ok(m);
    }

    @Transactional
    @PutMapping("/{id}/approve")
    public ResponseEntity<Void> approveCourse(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        Course c = courseRepository.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();

        c.setApprovalStatus("APPROVED");
        c.setApprovalNote(body != null ? body.get("note") : null);
        courseRepository.save(c);

        logAudit("COURSE_APPROVE", "COURSE", id, Map.of("note", body != null && body.get("note") != null ? body.get("note") : ""));
        return ResponseEntity.noContent().build();
    }

    @Transactional
    @PutMapping("/{id}/reject")
    public ResponseEntity<Void> rejectCourse(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Course c = courseRepository.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();

        c.setApprovalStatus("REJECTED");
        c.setApprovalNote(body.get("note"));
        courseRepository.save(c);

        logAudit("COURSE_REJECT", "COURSE", id, Map.of("note", body.getOrDefault("note", "")));
        return ResponseEntity.noContent().build();
    }

    @Transactional
    @PutMapping("/{id}/capacity")
    public ResponseEntity<Void> updateCapacity(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        Course c = courseRepository.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();

        Integer newCapacity = body.get("maxCapacity");
        if (newCapacity == null) return ResponseEntity.badRequest().build();
        c.setMaxCapacity(newCapacity);
        courseRepository.save(c);

        logAudit("CAPACITY_CHANGE", "COURSE", id, Map.of("newCapacity", String.valueOf(newCapacity)));
        return ResponseEntity.noContent().build();
    }

    @SuppressWarnings("unchecked")
    private void logAudit(String actionType, String targetType, Long targetId, Map<String, ?> details) {
        try {
            Long operatorId = SecurityUtil.getCurrentUserId();
            OperatorAuditLog auditLog = OperatorAuditLog.builder()
                    .operatorId(operatorId)
                    .actionType(actionType)
                    .targetType(targetType)
                    .targetId(targetId)
                    .details((Map<String, Object>) (Map<?, ?>) details)
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to write audit log: {} {} {}", actionType, targetType, targetId, e);
        }
    }
}
