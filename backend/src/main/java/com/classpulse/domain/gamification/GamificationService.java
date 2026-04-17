package com.classpulse.domain.gamification;

import com.classpulse.domain.course.Course;
import com.classpulse.domain.course.CourseRepository;
import com.classpulse.domain.twin.StudentTwin;
import com.classpulse.domain.twin.StudentTwinRepository;
import com.classpulse.domain.user.User;
import com.classpulse.domain.user.UserRepository;
import com.classpulse.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 게이미피케이션 서비스
 * XP 이벤트 처리, 뱃지 수여, 일일 스트릭 계산, 리더보드를 관리합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GamificationService {

    private final GamificationRepository gamificationRepository;
    private final XPEventRepository xpEventRepository;
    private final BadgeRepository badgeRepository;
    private final StudentBadgeRepository studentBadgeRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final StudentTwinRepository studentTwinRepository;
    private final NotificationService notificationService;

    // ── XP 이벤트별 기본 보상 ──────────────────────────────────────────

    private static final Map<String, Integer> DEFAULT_XP_AMOUNTS = Map.ofEntries(
            Map.entry("REFLECTION_SUBMIT", 20),
            Map.entry("REVIEW_COMPLETE", 15),
            Map.entry("QUIZ_PASS", 30),
            Map.entry("QUIZ_PERFECT", 50),
            Map.entry("CODE_SUBMIT", 25),
            Map.entry("CODE_REVIEW_GOOD", 40),
            Map.entry("CONSULTATION_ATTEND", 35),
            Map.entry("STREAK_7", 50),
            Map.entry("STREAK_14", 100),
            Map.entry("STREAK_30", 200),
            Map.entry("FIRST_REFLECTION", 30),
            Map.entry("STUDY_GROUP_JOIN", 20),
            Map.entry("CHATBOT_INTERACTION", 5)
    );

    /**
     * XP 이벤트를 추가하고 학생의 게이미피케이션 상태를 업데이트합니다.
     * 낙관적 락 충돌(동시 XP 수여) 시 최대 3회 재시도합니다.
     */
    public Map<String, Object> addXpEvent(Long studentId, Long courseId, String eventType,
                                           Integer amount, Long sourceId, String sourceType) {
        int attempts = 0;
        while (true) {
            try {
                return doAddXpEvent(studentId, courseId, eventType, amount, sourceId, sourceType);
            } catch (OptimisticLockingFailureException e) {
                attempts++;
                if (attempts >= 3) throw e;
                log.debug("XP award race on student={} course={}, retry {}/3", studentId, courseId, attempts);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Map<String, Object> doAddXpEvent(Long studentId, Long courseId, String eventType,
                                             Integer amount, Long sourceId, String sourceType) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found: " + courseId));

        int xpAmount = amount != null ? amount : DEFAULT_XP_AMOUNTS.getOrDefault(eventType, 10);

        // Create XP event
        XPEvent event = XPEvent.builder()
                .student(student)
                .course(course)
                .eventType(eventType)
                .xpAmount(xpAmount)
                .sourceId(sourceId)
                .sourceType(sourceType)
                .build();
        xpEventRepository.save(event);

        // Update gamification record
        StudentGamification gamification = gamificationRepository
                .findByStudentIdAndCourseId(studentId, courseId)
                .orElseGet(() -> {
                    StudentGamification newGamification = StudentGamification.builder()
                            .student(student)
                            .course(course)
                            .build();
                    return gamificationRepository.save(newGamification);
                });

        int previousLevel = gamification.getLevel();
        gamification.addXp(xpAmount);
        gamification.setLastActivityDate(LocalDate.now());
        gamificationRepository.save(gamification);

        boolean leveledUp = gamification.getLevel() > previousLevel;

        Map<String, Object> result = new HashMap<>();
        result.put("studentId", studentId);
        result.put("courseId", courseId);
        result.put("eventType", eventType);
        result.put("xpGained", xpAmount);
        result.put("currentXp", gamification.getCurrentXp());
        result.put("level", gamification.getLevel());
        result.put("levelTitle", gamification.getLevelTitle());
        result.put("nextLevelXp", gamification.getNextLevelXp());
        result.put("totalXpEarned", gamification.getTotalXpEarned());
        result.put("leveledUp", leveledUp);

        if (leveledUp) {
            log.info("레벨 업! studentId={}, level={}, title={}",
                    studentId, gamification.getLevel(), gamification.getLevelTitle());
        }

        return result;
    }

    /**
     * 학생의 뱃지 수여 여부를 확인하고 새로운 뱃지를 수여합니다.
     */
    @Transactional
    public List<Map<String, Object>> checkAndAwardBadges(Long studentId) {
        List<Badge> allBadges = badgeRepository.findAll();
        List<Map<String, Object>> newlyAwarded = new ArrayList<>();

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        List<XPEvent> events = xpEventRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
        List<StudentBadge> existingBadges = studentBadgeRepository.findByStudentId(studentId);
        Set<Long> earnedBadgeIds = existingBadges.stream()
                .map(sb -> sb.getBadge().getId())
                .collect(Collectors.toSet());

        for (Badge badge : allBadges) {
            if (earnedBadgeIds.contains(badge.getId())) continue;

            Map<String, Object> requirement = badge.getRequirementJson();
            if (requirement == null) continue;

            boolean earned = evaluateBadgeRequirement(studentId, requirement, events);
            if (earned) {
                StudentBadge studentBadge = StudentBadge.builder()
                        .student(student)
                        .badge(badge)
                        .build();
                studentBadgeRepository.save(studentBadge);

                newlyAwarded.add(Map.of(
                        "badgeId", badge.getId(),
                        "name", badge.getName(),
                        "emoji", badge.getEmoji() != null ? badge.getEmoji() : "",
                        "description", badge.getDescription() != null ? badge.getDescription() : ""
                ));

                // Notify student about earned badge
                notificationService.createNotification(
                        studentId,
                        "BADGE_EARNED",
                        (badge.getEmoji() != null ? badge.getEmoji() + " " : "") + "새 뱃지 획득!",
                        String.format("'%s' 뱃지를 획득했습니다! %s",
                                badge.getName(),
                                badge.getDescription() != null ? badge.getDescription() : ""),
                        Map.of("badgeId", badge.getId(), "badgeName", badge.getName())
                );

                log.info("뱃지 수여! studentId={}, badge={}", studentId, badge.getName());
            }
        }

        return newlyAwarded;
    }

    /**
     * 매일 자정에 모든 학생의 일일 스트릭을 계산합니다.
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void calculateDailyStreaks() {
        log.info("일일 스트릭 계산 시작");
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        List<StudentGamification> allGamifications = gamificationRepository.findAll();
        int resetCount = 0;
        int continueCount = 0;

        for (StudentGamification g : allGamifications) {
            if (g.getLastActivityDate() == null) {
                g.setStreakDays(0);
                continue;
            }

            if (g.getLastActivityDate().equals(yesterday) || g.getLastActivityDate().equals(today)) {
                // Streak continues (already incremented on activity)
                if (g.getLastActivityDate().equals(yesterday)) {
                    // Activity was yesterday, streak is still alive but not incremented today yet
                    continueCount++;
                }
            } else {
                // Streak broken
                if (g.getStreakDays() > 0) {
                    g.setStreakDays(0);
                    gamificationRepository.save(g);
                    resetCount++;
                }
            }
        }

        log.info("일일 스트릭 계산 완료 - 유지: {}명, 리셋: {}명", continueCount, resetCount);
    }

    /**
     * 과목별 리더보드를 조회합니다.
     */
    public List<Map<String, Object>> getLeaderboard(Long courseId) {
        List<StudentGamification> rankings = gamificationRepository
                .findByCourseIdOrderByTotalXpEarnedDesc(courseId);

        List<Map<String, Object>> leaderboard = new ArrayList<>();
        int rank = 1;
        for (StudentGamification g : rankings) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("rank", rank++);
            entry.put("studentId", g.getStudent().getId());
            entry.put("studentName", g.getStudent().getName());
            entry.put("level", g.getLevel());
            entry.put("levelTitle", g.getLevelTitle());
            entry.put("totalXp", g.getTotalXpEarned());
            entry.put("streakDays", g.getStreakDays());
            leaderboard.add(entry);
        }

        return leaderboard;
    }

    /**
     * 학생의 활동 스트릭을 업데이트합니다 (활동 발생 시 호출).
     */
    @Transactional
    public void updateStreak(Long studentId, Long courseId) {
        StudentGamification g = gamificationRepository
                .findByStudentIdAndCourseId(studentId, courseId).orElse(null);
        if (g == null) return;

        LocalDate today = LocalDate.now();
        if (today.equals(g.getLastActivityDate())) {
            return; // Already active today
        }

        if (g.getLastActivityDate() != null && g.getLastActivityDate().equals(today.minusDays(1))) {
            g.setStreakDays(g.getStreakDays() + 1);
        } else {
            g.setStreakDays(1);
        }

        g.setLastActivityDate(today);
        gamificationRepository.save(g);

        // Check streak milestones
        if (g.getStreakDays() == 7) {
            addXpEvent(studentId, courseId, "STREAK_7", null, null, "STREAK");
        } else if (g.getStreakDays() == 14) {
            addXpEvent(studentId, courseId, "STREAK_14", null, null, "STREAK");
        } else if (g.getStreakDays() == 30) {
            addXpEvent(studentId, courseId, "STREAK_30", null, null, "STREAK");
        }
    }

    // ── Badge evaluation helpers ─────────────────────────────────────────

    private boolean evaluateBadgeRequirement(Long studentId, Map<String, Object> requirement,
                                              List<XPEvent> events) {
        String type = (String) requirement.get("type");
        if (type == null) return false;

        switch (type) {
            // ── V3 seed format ──────────────────────────────────────────
            case "streak" -> {
                String target = (String) requirement.get("target");
                int days = ((Number) requirement.getOrDefault("days", 0)).intValue();

                if ("activity".equals(target)) {
                    // General activity streak
                    return gamificationRepository.findAll().stream()
                            .filter(g -> g.getStudent().getId().equals(studentId))
                            .anyMatch(g -> g.getStreakDays() >= days);
                }
                // For specific targets (review, reflection), check consecutive day events
                String eventType = mapTargetToEventType(target);
                if (eventType == null) return false;
                return countConsecutiveDays(events, eventType) >= days;
            }
            case "count" -> {
                String target = (String) requirement.get("target");
                int count = ((Number) requirement.getOrDefault("count", 0)).intValue();
                String eventType = mapTargetToEventType(target);
                if (eventType == null) return false;
                long actual = events.stream().filter(e -> eventType.equals(e.getEventType())).count();
                return actual >= count;
            }
            case "score" -> {
                String target = (String) requirement.get("target");
                int threshold = ((Number) requirement.getOrDefault("threshold", 0)).intValue();
                if ("mastery".equals(target)) {
                    // Check real mastery score from StudentTwin
                    List<StudentTwin> twins = studentTwinRepository.findByStudentId(studentId);
                    return twins.stream()
                            .anyMatch(t -> t.getMasteryScore().doubleValue() >= threshold);
                }
                return false;
            }
            case "action" -> {
                String target = (String) requirement.get("target");
                String eventType = mapTargetToEventType(target);
                if (eventType == null) return false;
                return events.stream().anyMatch(e -> eventType.equals(e.getEventType()));
            }
            // ── Legacy format (backward compatibility) ──────────────────
            case "EVENT_COUNT" -> {
                String eventType = (String) requirement.get("event_type");
                int threshold = ((Number) requirement.getOrDefault("count", 0)).intValue();
                long actual = events.stream().filter(e -> eventType.equals(e.getEventType())).count();
                return actual >= threshold;
            }
            case "TOTAL_XP" -> {
                int threshold = ((Number) requirement.getOrDefault("xp", 0)).intValue();
                int totalXp = events.stream().mapToInt(XPEvent::getXpAmount).sum();
                return totalXp >= threshold;
            }
            case "STREAK" -> {
                int threshold = ((Number) requirement.getOrDefault("days", 0)).intValue();
                return gamificationRepository.findAll().stream()
                        .filter(g -> g.getStudent().getId().equals(studentId))
                        .anyMatch(g -> g.getStreakDays() >= threshold);
            }
            case "LEVEL" -> {
                int threshold = ((Number) requirement.getOrDefault("level", 0)).intValue();
                return gamificationRepository.findAll().stream()
                        .filter(g -> g.getStudent().getId().equals(studentId))
                        .anyMatch(g -> g.getLevel() >= threshold);
            }
            default -> {
                log.warn("Unknown badge requirement type: {}", type);
                return false;
            }
        }
    }

    /**
     * Maps V3 seed target names to XP event types.
     */
    private String mapTargetToEventType(String target) {
        if (target == null) return null;
        return switch (target) {
            case "review" -> "REVIEW_COMPLETE";
            case "reflection" -> "REFLECTION_SUBMIT";
            case "code_submission" -> "CODE_SUBMIT";
            case "consultation" -> "CONSULTATION_ATTEND";
            case "code_feedback_applied" -> "CODE_REVIEW_GOOD";
            case "study_group_create" -> "STUDY_GROUP_JOIN";
            case "activity" -> null; // handled separately via streak
            default -> target.toUpperCase();
        };
    }

    /**
     * Counts the maximum consecutive days of a specific event type.
     */
    private int countConsecutiveDays(List<XPEvent> events, String eventType) {
        Set<LocalDate> eventDates = events.stream()
                .filter(e -> eventType.equals(e.getEventType()))
                .map(e -> e.getCreatedAt().toLocalDate())
                .collect(Collectors.toCollection(TreeSet::new));

        if (eventDates.isEmpty()) return 0;

        int maxStreak = 1;
        int currentStreak = 1;
        LocalDate previousDate = null;

        for (LocalDate date : eventDates) {
            if (previousDate != null && date.equals(previousDate.plusDays(1))) {
                currentStreak++;
                maxStreak = Math.max(maxStreak, currentStreak);
            } else if (previousDate != null && !date.equals(previousDate)) {
                currentStreak = 1;
            }
            previousDate = date;
        }

        return maxStreak;
    }
}
