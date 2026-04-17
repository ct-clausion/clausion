package com.classpulse.domain.gamification;

import com.classpulse.domain.user.User;
import com.classpulse.domain.course.Course;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "student_gamification", uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "course_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StudentGamification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Builder.Default
    private Integer level = 1;

    @Column(name = "current_xp")
    @Builder.Default
    private Integer currentXp = 0;

    @Column(name = "next_level_xp")
    @Builder.Default
    private Integer nextLevelXp = 100;

    @Column(name = "level_title", length = 50)
    @Builder.Default
    private String levelTitle = "초보 학습자";

    @Column(name = "streak_days")
    @Builder.Default
    private Integer streakDays = 0;

    @Column(name = "last_activity_date")
    private LocalDate lastActivityDate;

    @Column(name = "total_xp_earned")
    @Builder.Default
    private Integer totalXpEarned = 0;

    // Optimistic lock. Two concurrent XP awards for the same (student, course) can't
    // both read the same current_xp and overwrite each other — one of them will fail
    // with OptimisticLockingFailureException and retry.
    @Version
    @Column(name = "lock_version")
    @Builder.Default
    private Long lockVersion = 0L;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void addXp(int amount) {
        this.currentXp += amount;
        this.totalXpEarned += amount;
        while (this.currentXp >= this.nextLevelXp) {
            this.currentXp -= this.nextLevelXp;
            this.level++;
            this.nextLevelXp = (int)(100 * Math.pow(this.level, 1.2));
            this.levelTitle = calculateTitle(this.level);
        }
    }

    private String calculateTitle(int level) {
        if (level <= 3) return "초보 학습자";
        if (level <= 6) return "열정적 코더";
        if (level <= 10) return "풀스택 학습자";
        if (level <= 15) return "시니어 러너";
        return "마스터 개발자";
    }
}
