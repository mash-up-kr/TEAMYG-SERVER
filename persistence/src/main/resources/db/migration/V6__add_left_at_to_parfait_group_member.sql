ALTER TABLE parfait_group_member
    ADD COLUMN left_at DATETIME NULL AFTER joined_at,
    DROP INDEX uk_parfait_group_member_group_nickname;
