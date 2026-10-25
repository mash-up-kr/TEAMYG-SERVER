ALTER TABLE parfait_text
    ADD COLUMN is_fill BIT(1) NOT NULL DEFAULT b'0' AFTER font_size,
    ADD COLUMN is_stroke BIT(1) NOT NULL DEFAULT b'0' AFTER is_fill;
