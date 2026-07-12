CREATE TABLE member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    login_provider VARCHAR(255) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    global_nickname VARCHAR(255) NOT NULL,
    email VARCHAR(255) NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    deleted_at DATETIME NULL,
    CONSTRAINT uk_member_login_provider_provider_user_id UNIQUE (login_provider, provider_user_id)
);

CREATE TABLE image_meta (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    url VARCHAR(2048) NOT NULL,
    uploaded_by_member_id BIGINT NOT NULL,
    reference_count BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_image_meta_uploaded_by_member FOREIGN KEY (uploaded_by_member_id) REFERENCES member(id)
);

CREATE TABLE parfait_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    invite_code VARCHAR(255) NOT NULL,
    member_limit INT NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_parfait_group_invite_code UNIQUE (invite_code)
);

CREATE TABLE parfait_group_member (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parfait_group_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    group_nickname VARCHAR(255) NOT NULL,
    joined_at DATETIME NOT NULL,
    CONSTRAINT uk_parfait_group_member_group_member UNIQUE (parfait_group_id, member_id),
    CONSTRAINT uk_parfait_group_member_group_nickname UNIQUE (parfait_group_id, group_nickname),
    CONSTRAINT fk_parfait_group_member_group FOREIGN KEY (parfait_group_id) REFERENCES parfait_group(id),
    CONSTRAINT fk_parfait_group_member_member FOREIGN KEY (member_id) REFERENCES member(id)
);

CREATE TABLE parfait (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parfait_group_id BIGINT NOT NULL,
    parfait_date DATE NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT uk_parfait_group_parfait_date UNIQUE (parfait_group_id, parfait_date),
    CONSTRAINT fk_parfait_group FOREIGN KEY (parfait_group_id) REFERENCES parfait_group(id)
);

CREATE TABLE parfait_image (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parfait_id BIGINT NOT NULL,
    image_meta_id BIGINT NOT NULL,
    updated_by_member_id BIGINT NOT NULL,
    image_url VARCHAR(2048) NOT NULL,
    width INT NOT NULL,
    height INT NOT NULL,
    rotation DOUBLE NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_parfait_image_parfait FOREIGN KEY (parfait_id) REFERENCES parfait(id),
    CONSTRAINT fk_parfait_image_image_meta FOREIGN KEY (image_meta_id) REFERENCES image_meta(id),
    CONSTRAINT fk_parfait_image_updated_by_member FOREIGN KEY (updated_by_member_id) REFERENCES member(id)
);

CREATE TABLE parfait_group_reporting (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    parfait_group_id BIGINT NOT NULL,
    reporter_member_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_parfait_group_reporting_group FOREIGN KEY (parfait_group_id) REFERENCES parfait_group(id),
    CONSTRAINT fk_parfait_group_reporting_reporter FOREIGN KEY (reporter_member_id) REFERENCES member(id)
);

CREATE TABLE tos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(255) NOT NULL,
    version VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content LONGTEXT NOT NULL,
    required BIT(1) NOT NULL,
    published_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT uk_tos_type_version UNIQUE (type, version)
);

CREATE TABLE tos_agreement (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    tos_id BIGINT NOT NULL,
    agreed_at DATETIME NOT NULL,
    CONSTRAINT uk_tos_agreement_member_tos UNIQUE (member_id, tos_id),
    CONSTRAINT fk_tos_agreement_member FOREIGN KEY (member_id) REFERENCES member(id),
    CONSTRAINT fk_tos_agreement_tos FOREIGN KEY (tos_id) REFERENCES tos(id)
);
