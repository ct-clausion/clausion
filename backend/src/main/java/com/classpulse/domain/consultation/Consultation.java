package com.classpulse.domain.consultation;

import com.classpulse.domain.user.User;
import com.classpulse.domain.course.Course;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "consultations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Consultation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instructor_id", nullable = false)
    private User instructor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(length = 20)
    @Builder.Default
    private String status = "SCHEDULED";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "summary_text", columnDefinition = "TEXT")
    private String summaryText;

    @Column(name = "cause_analysis", columnDefinition = "TEXT")
    private String causeAnalysis;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "action_plan_json", columnDefinition = "jsonb")
    private List<Map<String, Object>> actionPlanJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "briefing_json", columnDefinition = "jsonb")
    private Map<String, Object> briefingJson;

    @Column(name = "video_room_name")
    private String videoRoomName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // Optimistic lock: prevents two concurrent status transitions (e.g. schedule + reject)
    // from silently overwriting one another.
    @Version
    @Column(name = "lock_version")
    private Long lockVersion;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (lockVersion == null) lockVersion = 0L;
    }
}
