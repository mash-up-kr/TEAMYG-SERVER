CREATE INDEX idx_parfait_group_member_member_group
    ON parfait_group_member (member_id, parfait_group_id);

CREATE INDEX idx_parfait_image_parfait_created_at
    ON parfait_image (parfait_id, created_at DESC);
