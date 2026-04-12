package com.classpulse.domain.intervention;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "intervention_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class InterventionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "operator_id", nullable = false)
    private Long operatorId;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "intervention_type", nullable = false)
    private String interventionType;  // CONSULTATION, MESSAGE, SUPPLEMENT, SCHEDULE_CHANGE

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "ai_suggested")
    private Boolean aiSuggested;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "twin_score_before", columnDefinition = "jsonb")
    private Map<String, Object> twinScoreBefore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "twin_score_after", columnDefinition = "jsonb")
    private Map<String, Object> twinScoreAfter;

    @Column(columnDefinition = "TEXT")
    private String outcome;

    private String status;  // PENDING, IN_PROGRESS, COMPLETED, INEFFECTIVE

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) status = "PENDING";
        if (aiSuggested == null) aiSuggested = false;
    }
}
