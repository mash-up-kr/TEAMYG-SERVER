CREATE TABLE device_token (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    token VARCHAR(512) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    session_id VARCHAR(64) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_device_token_token UNIQUE (token),
    CONSTRAINT fk_device_token_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE INDEX idx_device_token_member_session_id ON device_token (member_id, session_id);
