package com.classpulse.domain.twin;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "course_twin_snapshots")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CourseTwinSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "avg_mastery")
    private BigDecimal avgMastery;

    @Column(name = "avg_execution")
    private BigDecimal avgExecution;

    @Column(name = "avg_motivation")
    private BigDecimal avgMotivation;

    @Column(name = "retention_risk_ratio")
    private BigDecimal retentionRiskRatio;

    @Column(name = "attendance_rate")
    private BigDecimal attendanceRate;

    @Column(name = "completion_projection")
    private BigDecimal completionProjection;

    @Column(name = "health_score")
    private BigDecimal healthScore;

    @Column(name = "student_count")
    private Integer studentCount;

    @Column(name = "at_risk_count")
    private Integer atRiskCount;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
