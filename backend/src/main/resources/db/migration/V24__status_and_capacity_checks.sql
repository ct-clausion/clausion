-- Enforce enum-like string columns at the database layer so app bugs can't
-- persist arbitrary values (e.g., "INVALID"), and guard non-negative integer
-- business values (capacity, positive counts).

-- attendance_records.status whitelist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'attendance_status_check') THEN
        -- Repair any out-of-band values before adding the constraint.
        UPDATE attendance_records
        SET status = 'ABSENT'
        WHERE status NOT IN ('PRESENT', 'ABSENT', 'LATE', 'EXCUSED');

        ALTER TABLE attendance_records
            ADD CONSTRAINT attendance_status_check
                CHECK (status IN ('PRESENT', 'ABSENT', 'LATE', 'EXCUSED'));
    END IF;
END $$;

-- courses.max_capacity must be positive when set
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'courses_max_capacity_check') THEN
        UPDATE courses SET max_capacity = 30 WHERE max_capacity IS NOT NULL AND max_capacity <= 0;
        ALTER TABLE courses
            ADD CONSTRAINT courses_max_capacity_check
                CHECK (max_capacity IS NULL OR max_capacity > 0);
    END IF;
END $$;

-- consultations.status whitelist — catches typos from controller code.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'consultations_status_check') THEN
        UPDATE consultations
        SET status = 'REQUESTED'
        WHERE status NOT IN ('REQUESTED', 'SCHEDULED', 'IN_PROGRESS', 'COMPLETED',
                             'CANCELLED', 'REJECTED', 'VIDEO_ENDED');
        ALTER TABLE consultations
            ADD CONSTRAINT consultations_status_check
                CHECK (status IN ('REQUESTED', 'SCHEDULED', 'IN_PROGRESS', 'COMPLETED',
                                  'CANCELLED', 'REJECTED', 'VIDEO_ENDED'));
    END IF;
END $$;
