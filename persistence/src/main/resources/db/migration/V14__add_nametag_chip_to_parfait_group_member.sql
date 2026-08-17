ALTER TABLE parfait_group_member ADD COLUMN nametag_chip VARCHAR(10) NULL;

-- 이미 존재하는 활성 멤버(left_at IS NULL)에게 그룹별로 겹치지 않는 타입을 랜덤 배정한다.
-- 그룹 정원 최대치(12)와 타입 개수(12)가 같아서 그룹당 순번 1~n(n<=12)이 항상 TYPE1~12
-- 범위 안에 들어온다.
WITH ranked AS (
    SELECT id, ROW_NUMBER() OVER (PARTITION BY parfait_group_id ORDER BY RAND()) AS rn
    FROM parfait_group_member
    WHERE left_at IS NULL
)
UPDATE parfait_group_member m
JOIN ranked r ON r.id = m.id
SET m.nametag_chip = CONCAT('TYPE', r.rn);

-- 이미 탈퇴한 기존 멤버들도 RELEASED로 채워 이후 탈퇴자와 동일하게 취급한다
-- (leave() 도메인 로직이 RELEASED를 반납값으로 쓰는 것과 일관성을 맞춘다).
UPDATE parfait_group_member
SET nametag_chip = 'RELEASED'
WHERE left_at IS NOT NULL;
