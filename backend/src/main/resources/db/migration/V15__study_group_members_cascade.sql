-- Add ON DELETE CASCADE to study_group_members FK for safe group deletion
ALTER TABLE study_group_members
    DROP CONSTRAINT study_group_members_study_group_id_fkey,
    ADD CONSTRAINT study_group_members_study_group_id_fkey
        FOREIGN KEY (study_group_id) REFERENCES study_groups(id) ON DELETE CASCADE;
