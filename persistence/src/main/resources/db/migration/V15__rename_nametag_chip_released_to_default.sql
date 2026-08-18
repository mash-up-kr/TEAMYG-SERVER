UPDATE parfait_group_member
SET nametag_chip = 'DEFAULT'
WHERE nametag_chip = 'RELEASED';

ALTER TABLE parfait_group_member
    MODIFY COLUMN nametag_chip VARCHAR(10) NOT NULL DEFAULT 'DEFAULT';
