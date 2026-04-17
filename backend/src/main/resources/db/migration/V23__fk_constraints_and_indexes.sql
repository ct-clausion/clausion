-- B27: Add proper FK constraints with ON DELETE CASCADE for orphan-prevention.
-- B28: Add indexes for frequent filter columns to avoid full table scans.
--
-- These columns were historically stored as bare BIGINT without FK — deleting a
-- parent (course, session, user) left dangling rows silently. The constraints
-- below close that gap without changing the JPA mapping style.

-- ──────────────────────────────────────────────────────────
-- attendance_records.session_id → course_sessions.id
-- attendance_records.student_id → users.id
-- ──────────────────────────────────────────────────────────
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_attendance_session') THEN
        ALTER TABLE attendance_records
            ADD CONSTRAINT fk_attendance_session
            FOREIGN KEY (session_id) REFERENCES course_sessions(id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_attendance_student') THEN
        ALTER TABLE attendance_records
            ADD CONSTRAINT fk_attendance_student
            FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_attendance_session ON attendance_records(session_id);
CREATE INDEX IF NOT EXISTS idx_attendance_student ON attendance_records(student_id);
CREATE INDEX IF NOT EXISTS idx_attendance_session_student
    ON attendance_records(session_id, student_id);

-- ──────────────────────────────────────────────────────────
-- course_sessions.course_id → courses.id
-- ──────────────────────────────────────────────────────────
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_course_sessions_course') THEN
        ALTER TABLE course_sessions
            ADD CONSTRAINT fk_course_sessions_course
            FOREIGN KEY (course_id) REFERENCES courses(id) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_course_sessions_course ON course_sessions(course_id);

-- ──────────────────────────────────────────────────────────
-- course_enrollments: lookup by course is the dominant query shape
-- ──────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_course_enrollments_course ON course_enrollments(course_id);
CREATE INDEX IF NOT EXISTS idx_course_enrollments_course_status
    ON course_enrollments(course_id, status);

-- ──────────────────────────────────────────────────────────
-- student_gamification / xp_events → ON DELETE CASCADE when course is removed.
-- Existing FKs (if any) are kept; we only add cascade-style guarantees if missing.
-- ──────────────────────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_student_gamification_course ON student_gamification(course_id);
CREATE INDEX IF NOT EXISTS idx_xp_events_student_course
    ON xp_events(student_id, course_id);
