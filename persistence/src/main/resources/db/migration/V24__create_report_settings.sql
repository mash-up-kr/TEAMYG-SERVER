CREATE TABLE report_settings (
    id BIGINT NOT NULL PRIMARY KEY,
    ad_mode BOOLEAN NOT NULL DEFAULT FALSE,
    interval_days INT NOT NULL DEFAULT 3,
    last_sent_at DATETIME NULL,
    updated_at DATETIME NOT NULL
);

INSERT INTO report_settings (id, ad_mode, interval_days, last_sent_at, updated_at)
VALUES (1, FALSE, 3, NULL, CURRENT_TIMESTAMP);
