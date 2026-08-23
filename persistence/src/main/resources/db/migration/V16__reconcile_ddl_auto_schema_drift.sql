-- Flyway가 꺼져 있던 기간(V4 적용 이후 ~ #110) 동안 ddl-auto: update가 스키마를 대신 관리하면서
-- 생긴 드리프트를 보정한다. ddl-auto: update는 컬럼·인덱스를 추가만 하고 삭제하지 않으므로,
-- DROP을 포함한 V5·V8·V10과 제약을 바꾸는 V7·V11·V15의 효과가 운영 DB에 반영되지 않았다.
--
-- 신규 DB(V1~V15가 정상 적용된 상태)에서도 안전하게 실행되도록, 존재 여부에 따라 갈리는 구문은
-- information_schema를 조회해 조건부로 실행한다. MySQL은 DROP COLUMN/DROP INDEX에 IF EXISTS를
-- 지원하지 않아 동적 SQL을 쓴다.

-- 1. V5·V10이 지우려던 고아 컬럼 제거
SET @sql := IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'member' AND COLUMN_NAME = 'email'),
    'ALTER TABLE member DROP COLUMN email',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql := IF(
    EXISTS(SELECT 1 FROM information_schema.COLUMNS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'member' AND COLUMN_NAME = 'apple_refresh_token'),
    'ALTER TABLE member DROP COLUMN apple_refresh_token',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. V6이 지우려던 유니크 제약 제거. V2가 만들고 V6이 DROP하는데, V6이 적용되지 않아
--    운영에는 남아 있다. 코드에 그룹 내 닉네임 중복 검사가 없어 중복 시 처리되지 않은 500을 유발한다.
SET @sql := IF(
    EXISTS(SELECT 1 FROM information_schema.STATISTICS
           WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'parfait_group_member'
             AND INDEX_NAME = 'uk_parfait_group_member_group_nickname'),
    'ALTER TABLE parfait_group_member DROP INDEX uk_parfait_group_member_group_nickname',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. V8이 추가하려던 FK 복원. ddl-auto는 평범한 Long 컬럼에 FK를 만들지 않는다.
SET @sql := IF(
    NOT EXISTS(SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'parfait_image'
                 AND CONSTRAINT_NAME = 'fk_parfait_image_placed_by_group_member'),
    'ALTER TABLE parfait_image
         ADD CONSTRAINT fk_parfait_image_placed_by_group_member
         FOREIGN KEY (placed_by_group_member_id) REFERENCES parfait_group_member(id)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. V7·V11이 지정한 기본값 복원. ddl-auto가 만든 컬럼에는 DEFAULT가 없다.
ALTER TABLE image_meta
    ALTER COLUMN image_type SET DEFAULT 'BACKGROUND',
    ALTER COLUMN status SET DEFAULT 'PENDING';

ALTER TABLE parfait
    ALTER COLUMN status SET DEFAULT 'ACTIVE';

-- 5. V14가 하려던 nametag_chip 백필 복원.
--    V14는 컬럼 추가뿐 아니라 "기존 활성 멤버에게 그룹 내에서 겹치지 않는 TYPEn을 배정한다"는
--    데이터 이관까지 포함한다. ddl-auto는 컬럼만 만들었으므로 그 시점에 이미 있던 멤버의 값은
--    비어 있다(컬럼을 언제 어떤 정의로 만들었는지에 따라 NULL 또는 빈 문자열).
--    전부 'DEFAULT'로 채우면 활성 멤버가 탈퇴 멤버용 칩을 갖게 되므로
--    (NameTagChipType.DEFAULT는 assignRandom의 배정 후보에서 제외되는 값이다) V14와 같은 규칙으로 되살린다.
--    열거형에 없는 값은 읽는 순간 valueOf가 터지므로 NULL·빈 문자열을 한데 묶어 미배정으로 본다.

-- 5-1. 활성 멤버: 그룹 안에서 아직 쓰이지 않은 TYPEn을 순서대로 배정한다.
WITH RECURSIVE
    chip_number AS (
        SELECT 1 AS n
        UNION ALL
        SELECT n + 1 FROM chip_number WHERE n < 12
    ),
    unassigned AS (
        SELECT id,
               parfait_group_id,
               ROW_NUMBER() OVER (PARTITION BY parfait_group_id ORDER BY id) AS seq
        FROM parfait_group_member
        WHERE left_at IS NULL
          AND (nametag_chip IS NULL OR nametag_chip NOT REGEXP '^(TYPE([1-9]|1[0-2])|DEFAULT)$')
    ),
    vacancy AS (
        SELECT target.parfait_group_id,
               CONCAT('TYPE', chip_number.n) AS chip,
               ROW_NUMBER() OVER (PARTITION BY target.parfait_group_id ORDER BY chip_number.n) AS seq
        FROM (SELECT DISTINCT parfait_group_id FROM unassigned) AS target
        CROSS JOIN chip_number
        WHERE NOT EXISTS (
            SELECT 1
            FROM parfait_group_member occupant
            WHERE occupant.parfait_group_id = target.parfait_group_id
              AND occupant.left_at IS NULL
              AND occupant.nametag_chip = CONCAT('TYPE', chip_number.n)
        )
    )
UPDATE parfait_group_member m
JOIN unassigned u ON u.id = m.id
JOIN vacancy v ON v.parfait_group_id = u.parfait_group_id AND v.seq = u.seq
SET m.nametag_chip = v.chip;

-- 5-2. 탈퇴 멤버와, 빈 칩이 남지 않아 5-1에서 배정받지 못한 멤버는 DEFAULT로 수렴시킨다.
UPDATE parfait_group_member
SET nametag_chip = 'DEFAULT'
WHERE nametag_chip IS NULL
   OR nametag_chip NOT REGEXP '^(TYPE([1-9]|1[0-2])|DEFAULT)$';

-- 5-3. V15의 NOT NULL과 V6의 left_at 정밀도 복원.
ALTER TABLE parfait_group_member
    MODIFY COLUMN left_at DATETIME NULL,
    MODIFY COLUMN nametag_chip VARCHAR(10) NOT NULL DEFAULT 'DEFAULT';
