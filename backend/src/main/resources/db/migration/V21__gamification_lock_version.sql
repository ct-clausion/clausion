-- Optimistic lock version for student_gamification. Prevents lost XP updates
-- when two award events arrive concurrently for the same (student, course).
ALTER TABLE student_gamification
    ADD COLUMN lock_version BIGINT NOT NULL DEFAULT 0;
