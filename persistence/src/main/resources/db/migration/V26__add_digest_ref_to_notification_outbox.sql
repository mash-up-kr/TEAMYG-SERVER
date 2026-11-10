ALTER TABLE notification_outbox ADD COLUMN digest_ref VARCHAR(150) NULL;
CREATE INDEX idx_notification_outbox_digest_ref ON notification_outbox (digest_ref);
