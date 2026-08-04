ALTER TABLE parfait_image
    DROP FOREIGN KEY fk_parfait_image_updated_by_member,
    DROP COLUMN updated_by_member_id,
    DROP COLUMN width,
    DROP COLUMN height,
    ADD COLUMN placed_by_group_member_id BIGINT NOT NULL AFTER image_meta_id,
    ADD COLUMN position_x DOUBLE NOT NULL AFTER image_url,
    ADD COLUMN position_y DOUBLE NOT NULL AFTER position_x,
    ADD COLUMN position_z INT NOT NULL AFTER position_y,
    ADD COLUMN scale DOUBLE NOT NULL AFTER position_z,
    ADD COLUMN border_type VARCHAR(50) NOT NULL AFTER rotation,
    ADD COLUMN border_color VARCHAR(20) NULL AFTER border_type,
    ADD COLUMN border_width DOUBLE NULL AFTER border_color,
    ADD CONSTRAINT fk_parfait_image_placed_by_group_member
        FOREIGN KEY (placed_by_group_member_id) REFERENCES parfait_group_member(id),
    ADD CONSTRAINT uk_parfait_image_parfait_image_meta UNIQUE (parfait_id, image_meta_id);
