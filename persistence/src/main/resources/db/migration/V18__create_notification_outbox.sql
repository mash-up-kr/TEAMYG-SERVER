CREATE TABLE notification_outbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    aggregate_type VARCHAR(40) NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR(60) NOT NULL,
    receiver_member_id BIGINT NOT NULL,
    payload LONGTEXT NOT NULL,
    dedup_key VARCHAR(150) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts INT NOT NULL DEFAULT 0,
    scheduled_at DATETIME NOT NULL,
    last_error VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    sent_at DATETIME NULL,
    CONSTRAINT uk_notification_outbox_dedup UNIQUE (dedup_key)
);

CREATE INDEX idx_notification_outbox_poll ON notification_outbox (status, scheduled_at);
