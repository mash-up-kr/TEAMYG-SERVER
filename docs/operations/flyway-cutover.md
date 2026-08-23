# Flyway 전환 런북 (#110)

운영 DB를 `ddl-auto: update` 관리에서 Flyway 단독 관리로 넘기는 1회성 절차. 전환이 끝나면 이 문서는
기록으로만 남는다.

## 배경

- 운영 `flyway_schema_history`에는 **V1~V4까지만** 기록되어 있다(마지막 적용 2026-08-01).
- 그 이후 스키마는 `spring.jpa.hibernate.ddl-auto: update`가 대신 만들어 왔다.
- `update`는 컬럼·인덱스를 **추가만 하고 삭제하지 않는다.** 그래서 DROP을 포함한 V5·V6·V8·V10과
  제약·기본값을 바꾸는 V7·V11·V15의 효과가 운영에 반영되지 않았고, `parfait_image` INSERT가
  `updated_by_member_id doesn't have a default value`로 실패했다(급한 불은 V8의 DROP만 수동 적용해 끔).

## 왜 그냥 `flyway.enabled: true`로 켜면 안 되는가

히스토리에 V1~V4만 있으므로 Flyway는 **V5부터 다시 실행하려 한다.** 이미 `ddl-auto`가 만들어 둔
컬럼을 V6·V7·V9·V11·V14가 다시 ADD 하다가 `Duplicate column name`으로 죽는다. 그래서

1. V5~V15를 **이미 적용된 것으로 기록(baseline)** 하고,
2. `ddl-auto`가 못 따라온 차이만 **V16**이 메우도록 한다.

V16은 신규 DB에서도 실행되므로 존재 여부에 따라 갈리는 구문은 `information_schema`를 보고 조건부로
실행한다. 빈 DB 경로와 드리프트 경로의 최종 스키마가 정확히 같다는 것은
`FlywayMigrationTest`가 컬럼·인덱스·제약 스냅샷을 비교해 검증한다.

## 절차

### 0. 백업

```sh
mysqldump --single-transaction --routines --triggers -h "$DB_HOST" -u "$DB_USERNAME" -p parfait \
  > parfait-before-110.sql
```

### 1. 사전 점검

히스토리가 V1~V4에서 끊겨 있고 체크섬이 저장소와 같은지 확인한다. 다르면 여기서 멈추고 원인을 먼저 밝힌다.

```sql
SELECT installed_rank, version, script, checksum, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

| version | checksum    |
| ------- | ----------- |
| 1       | -997806263  |
| 2       | -659035850  |
| 3       | -1689428606 |
| 4       | -639281652  |

이어서 전환 전 스키마를 떠 둔다. 4번 검증에서 예상과 대조하고, 문제가 생기면 진단 근거가 된다.

```sql
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
ORDER BY TABLE_NAME, COLUMN_NAME;
```

### 2. V5~V15를 적용된 것으로 기록

`installed_rank`는 4 다음부터 이어진다. 체크섬은 저장소의 마이그레이션 파일에서 Flyway가 계산한 값
그대로이며, 파일을 한 글자라도 고치면 달라지므로 **마이그레이션 파일을 수정했다면 이 표를 다시 뽑아야 한다.**

```sql
INSERT INTO flyway_schema_history
    (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success)
VALUES
    ( 5,  '5', 'drop member email',                       'SQL', 'V5__drop_member_email.sql',                        -97277144, 'baseline-110', 0, 1),
    ( 6,  '6', 'add left at to parfait group member',     'SQL', 'V6__add_left_at_to_parfait_group_member.sql',    -1966879986, 'baseline-110', 0, 1),
    ( 7,  '7', 'add image type and status to image meta', 'SQL', 'V7__add_image_type_and_status_to_image_meta.sql', -756688310, 'baseline-110', 0, 1),
    ( 8,  '8', 'redesign parfait image for placement',    'SQL', 'V8__redesign_parfait_image_for_placement.sql',   -1737724911, 'baseline-110', 0, 1),
    ( 9,  '9', 'add apple refresh token to member',       'SQL', 'V9__add_apple_refresh_token_to_member.sql',      -1360542916, 'baseline-110', 0, 1),
    (10, '10', 'drop apple refresh token from member',    'SQL', 'V10__drop_apple_refresh_token_from_member.sql',  -1418581321, 'baseline-110', 0, 1),
    (11, '11', 'add status and background to parfait',    'SQL', 'V11__add_status_and_background_to_parfait.sql',   1828668881, 'baseline-110', 0, 1),
    (12, '12', 'add spring batch schema',                 'SQL', 'V12__add_spring_batch_schema.sql',                -709189580, 'baseline-110', 0, 1),
    (13, '13', 'shrink parfait group invite code length', 'SQL', 'V13__shrink_parfait_group_invite_code_length.sql', 560789938, 'baseline-110', 0, 1),
    (14, '14', 'add nametag chip to parfait group member','SQL', 'V14__add_nametag_chip_to_parfait_group_member.sql',-1255325880, 'baseline-110', 0, 1),
    (15, '15', 'rename nametag chip released to default', 'SQL', 'V15__rename_nametag_chip_released_to_default.sql', -633711512, 'baseline-110', 0, 1);
```

V12(Spring Batch 메타테이블)는 그동안 `spring.batch.jdbc.initialize-schema: always` stopgap이 만들어
왔다. V12는 Spring Batch가 배포하는 공식 스크립트를 그대로 넣은 파일이라 결과가 같으므로 적용된 것으로
기록해도 된다. 이번 배포에서 그 stopgap은 `never`로 되돌아가고 메타테이블 소유권은 V12로 넘어간다.

### 3. 배포

이 PR이 배포되면 `spring.flyway.enabled: true`, `spring.jpa.hibernate.ddl-auto: validate`로 기동하면서
**V16만** 실행된다. V16이 하는 일:

| 항목                                                    | 원래 주인   |
| ------------------------------------------------------- | ----------- |
| `member.email`, `member.apple_refresh_token` 제거        | V5, V10     |
| `uk_parfait_group_member_group_nickname` 제거            | V6          |
| `fk_parfait_image_placed_by_group_member` 복원           | V8          |
| `image_meta.image_type`/`status`, `parfait.status` 기본값 | V7, V11     |
| `nametag_chip` 백필과 NOT NULL, `left_at` 정밀도          | V14, V15, V6 |

`nametag_chip` 백필은 값이 비어 있는(NULL·빈 문자열) **활성** 멤버에게 그룹 안에서 쓰이지 않은
`TYPEn`을 배정하고, 탈퇴 멤버만 `DEFAULT`로 채운다. `DEFAULT`는 탈퇴 멤버용 값이라 활성 멤버에게
일괄로 넣으면 안 된다.

### 4. 검증

- 기동 로그에 `Migrating schema ... to version 16`과 정상 기동이 찍히는지. `ddl-auto: validate`가
  켜져 있으므로 스키마와 엔티티가 어긋나면 **여기서 기동이 실패한다.** 그게 이 설정의 목적이다.
- 발단이 된 API가 통과하는지: `POST /api/v1/groups/{groupId}/parfaits/{parfaitId}/images`
- 배치 메타테이블이 살아 있는지: `SELECT COUNT(*) FROM BATCH_JOB_INSTANCE;`
- 1번에서 떠 둔 스키마와 대조해, V16이 손대지 않은 항목 중 예상 밖 차이가 없는지.

### 5. 롤백

V16은 DROP COLUMN을 포함하므로 되돌리려면 0번 백업에서 복원한다. `flyway_schema_history`에서 행만
지우는 것으로는 컬럼이 돌아오지 않는다. 다만 V16이 지우는 컬럼은 코드가 더 이상 읽지 않는 값이고,
애플리케이션을 이전 버전으로 되돌리는 것만으로는 `ddl-auto: update`가 컬럼을 **다시 추가**할 뿐이므로
(값은 비어 있다) 대개 앞으로 고치는 편이 빠르다.

## 전환 이후

- 스키마 변경은 마이그레이션 파일로만 한다. `ddl-auto: validate`는 어긋남을 기동 시점에 잡아 줄 뿐,
  고쳐 주지는 않는다.
- 마이그레이션을 추가하면 `FlywayMigrationTest`가 빈 DB 적용을 검증하고,
  `ParfaitCanvasRotationJobIntegrationTest`가 실제 배포 설정 그대로 종단 기동을 검증한다.