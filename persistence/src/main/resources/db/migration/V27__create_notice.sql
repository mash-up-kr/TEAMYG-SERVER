CREATE TABLE notice (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    version VARCHAR(20) NULL,
    revision_target VARCHAR(20) NULL,
    title VARCHAR(255) NOT NULL,
    content LONGTEXT NOT NULL,
    published_date DATE NOT NULL,
    created_at DATETIME NOT NULL
);

CREATE INDEX idx_notice_platform_published ON notice (platform, published_date DESC, id DESC);
