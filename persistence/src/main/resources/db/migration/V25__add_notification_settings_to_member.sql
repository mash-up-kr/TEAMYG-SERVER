ALTER TABLE member
    ADD COLUMN daily_reminder_enabled TINYINT(1) NOT NULL DEFAULT 1,
    ADD COLUMN activity_digest_enabled TINYINT(1) NOT NULL DEFAULT 0;
