CREATE TABLE app_version (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    platform        VARCHAR(10)  NOT NULL,
    minimum_version VARCHAR(20)  NOT NULL,
    latest_version  VARCHAR(20)  NOT NULL,
    store_url       VARCHAR(500),
    PRIMARY KEY (id),
    UNIQUE KEY uk_app_version_platform (platform)
);
