CREATE TABLE parfait_text (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parfait_id BIGINT NOT NULL,
    placed_by_group_member_id BIGINT NOT NULL,
    text VARCHAR(2000) NOT NULL,
    position_x DOUBLE NOT NULL,
    position_y DOUBLE NOT NULL,
    position_z INT NOT NULL,
    scale DOUBLE NOT NULL,
    rotation DOUBLE NOT NULL,
    color VARCHAR(20) NOT NULL,
    font_size DOUBLE NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
);

CREATE INDEX idx_parfait_text_parfait_id ON parfait_text (parfait_id);
