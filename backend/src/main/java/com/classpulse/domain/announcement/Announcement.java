package com.classpulse.domain.announcement;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "announcements")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "target_type", nullable = false)
    private String targetType;  // ALL, COURSE, INSTRUCTOR_ONLY, STUDENT_ONLY

    @Column(name = "target_course_id")
    private Long targetCourseId;

    @Column(name = "is_urgent")
    private Boolean isUrgent;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (targetType == null) targetType = "ALL";
        if (isUrgent == null) isUrgent = false;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
