ALTER TABLE parfait
    ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' AFTER parfait_date,
    ADD COLUMN background_type VARCHAR(50) NULL AFTER status,
    ADD COLUMN background_value VARCHAR(2048) NULL AFTER background_type;
