-- V12: Re-introduce OPERATOR role with proper security model
-- History: V8 removed ADMIN/OPERATOR roles. Re-adding OPERATOR as a distinct
-- education operations role with course-scoped access and AI copilot features.

-- 1. Operator profiles
CREATE TABLE operator_profiles (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) NOT NULL UNIQUE,
    department VARCHAR(100),
    position VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 2. Operator-Course assignments (normalized join table, not JSONB)
CREATE TABLE operator_course_assignments (
    id BIGSERIAL PRIMARY KEY,
    operator_id BIGINT REFERENCES users(id) NOT NULL,
    course_id BIGINT REFERENCES courses(id) NOT NULL,
    assigned_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(operator_id, course_id)
);

-- 3. Course sessions (for attendance tracking)
CREATE TABLE course_sessions (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT REFERENCES courses(id) NOT NULL,
    session_date DATE NOT NULL,
    session_number INTEGER NOT NULL DEFAULT 1,
    title VARCHAR(200),
    status VARCHAR(20) DEFAULT 'SCHEDULED',
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(course_id, session_date, session_number)
);

-- 4. Attendance records
CREATE TABLE attendance_records (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT REFERENCES course_sessions(id) NOT NULL,
    student_id BIGINT REFERENCES users(id) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ABSENT',
    check_in_time TIMESTAMP,
    recorded_by BIGINT REFERENCES users(id),
    note TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(session_id, student_id)
);
CREATE INDEX idx_attendance_student ON attendance_records(student_id);
CREATE INDEX idx_attendance_session ON attendance_records(session_id);

-- 5. Announcements
CREATE TABLE announcements (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    target_type VARCHAR(20) NOT NULL DEFAULT 'ALL',
    target_course_id BIGINT REFERENCES courses(id),
    is_urgent BOOLEAN DEFAULT FALSE,
    author_id BIGINT REFERENCES users(id) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE announcement_reads (
    id BIGSERIAL PRIMARY KEY,
    announcement_id BIGINT REFERENCES announcements(id) NOT NULL,
    user_id BIGINT REFERENCES users(id) NOT NULL,
    read_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(announcement_id, user_id)
);

-- 6. Intervention logs
CREATE TABLE intervention_logs (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT REFERENCES users(id) NOT NULL,
    operator_id BIGINT REFERENCES users(id) NOT NULL,
    course_id BIGINT REFERENCES courses(id),
    intervention_type VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    ai_suggested BOOLEAN DEFAULT FALSE,
    twin_score_before JSONB,
    twin_score_after JSONB,
    outcome TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT NOW(),
    completed_at TIMESTAMP
);
CREATE INDEX idx_intervention_student ON intervention_logs(student_id);
CREATE INDEX idx_intervention_status ON intervention_logs(status);

-- 7. Operator audit logs
CREATE TABLE operator_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    operator_id BIGINT REFERENCES users(id) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    target_type VARCHAR(50) NOT NULL,
    target_id BIGINT NOT NULL,
    details JSONB,
    created_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX idx_audit_operator ON operator_audit_logs(operator_id);
CREATE INDEX idx_audit_created ON operator_audit_logs(created_at);

-- 8. Operation reports
CREATE TABLE operation_reports (
    id BIGSERIAL PRIMARY KEY,
    report_type VARCHAR(50) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    summary_json JSONB NOT NULL,
    anomalies_json JSONB,
    recommendations_json JSONB,
    generated_at TIMESTAMP DEFAULT NOW()
);

-- 9. Course Twin snapshots
CREATE TABLE course_twin_snapshots (
    id BIGSERIAL PRIMARY KEY,
    course_id BIGINT REFERENCES courses(id) NOT NULL,
    avg_mastery DECIMAL(5,2),
    avg_execution DECIMAL(5,2),
    avg_motivation DECIMAL(5,2),
    retention_risk_ratio DECIMAL(5,4),
    attendance_rate DECIMAL(5,4),
    completion_projection DECIMAL(5,4),
    health_score DECIMAL(5,2),
    student_count INTEGER,
    at_risk_count INTEGER,
    snapshot_date DATE NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(course_id, snapshot_date)
);
CREATE INDEX idx_course_twin_course ON course_twin_snapshots(course_id);

-- 10. Course approval fields
ALTER TABLE courses ADD COLUMN IF NOT EXISTS approval_status VARCHAR(20) DEFAULT 'APPROVED';
ALTER TABLE courses ADD COLUMN IF NOT EXISTS approval_note TEXT;
ALTER TABLE courses ADD COLUMN IF NOT EXISTS max_capacity INTEGER DEFAULT 30;

-- 11. Seed operator account (password: operator123)
INSERT INTO users (email, username, name, password_hash, role, created_at, updated_at)
VALUES ('operator@clausion.com', 'operator', '교육운영자',
        '$2b$10$iTjSwrpk1WLjXFSD1mEVTe7FbTSgtTWpZXLWQNuf1cbvE4fYW6yN2',
        'OPERATOR', NOW(), NOW())
ON CONFLICT (email) DO NOTHING;

INSERT INTO operator_profiles (user_id, department, position)
SELECT id, '교육운영팀', '운영 총괄'
FROM users WHERE email = 'operator@clausion.com'
ON CONFLICT (user_id) DO NOTHING;
