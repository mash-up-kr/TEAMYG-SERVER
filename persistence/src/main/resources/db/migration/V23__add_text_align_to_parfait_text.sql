ALTER TABLE parfait_text
    ADD COLUMN text_align VARCHAR(20) NOT NULL DEFAULT 'CENTER' AFTER is_stroke;
