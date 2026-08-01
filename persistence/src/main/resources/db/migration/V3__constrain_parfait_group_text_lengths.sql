ALTER TABLE parfait_group
    MODIFY COLUMN name VARCHAR(10) NOT NULL,
    MODIFY COLUMN invite_code VARCHAR(8) NOT NULL,
    ADD CONSTRAINT chk_parfait_group_member_limit CHECK (member_limit BETWEEN 1 AND 12);

ALTER TABLE parfait_group_member
    MODIFY COLUMN group_nickname VARCHAR(15) NOT NULL;
