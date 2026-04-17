-- Optimistic lock for consultations. Blocks lost-update anomalies when two
-- state transitions (e.g. schedule + reject) arrive concurrently.
ALTER TABLE consultations
    ADD COLUMN lock_version BIGINT NOT NULL DEFAULT 0;
