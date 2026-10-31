# 가입자 리포트 디스코드 봇 운영 (#205)

3일마다 21:00 KST에 가입자 리포트를 디스코드 채널로 보내고, 같은 통계를 `GET /admin/stats`로 조회한다.

## 환경변수

| 변수 | 용도 | 비워 두면 |
|---|---|---|
| `DISCORD_BOT_TOKEN` | 봇 토큰(디스코드 개발자 포털 → Bot) | 봇을 켜지 않음 |
| `DISCORD_REPORT_CHANNEL_ID` | 리포트를 올릴 채널 ID | 전송 실패(ERROR 로그) |
| `DISCORD_GUILD_ID` | 슬래시 명령어를 등록할 디스코드 서버 ID | 명령어를 등록하지 않음 |
| `DISCORD_ADMIN_ROLE_ID` | `/광고모드` 등 운영진 전용 명령어를 쓸 역할 ID | 운영진 전용 명령어 사용 불가 |
| `REPORT_ENABLED` | `true`면 정기 리포트 스케줄러 켬 | 꺼짐 |
| `REPORT_HOUR` | 정기 리포트 전송 시각(KST, 0~23) | 미정의 시 21시. 빈 값(`REPORT_HOUR=`)으로 두면 리포트를 꺼 둬도 앱이 뜨지 않는다. 범위 0~23 |
| `REPORT_EXCLUDED_MEMBER_IDS` | 집계에서 뺄 테스트·운영진 member id(콤마 구분) | 제외 없음 |
| `STATS_API_TOKEN` | 통계 API 인증 토큰(랜덤 문자열) | 통계 API가 모든 요청 거절 |

- `REPORT_HOUR`는 값을 넣지 않을 거면 `.env`에서 줄 자체를 지우세요. `REPORT_HOUR=`처럼 빈 값으로 두면 `/광고모드`의 다음 리포트 계산이 이 값을 항상 읽기 때문에 `REPORT_ENABLED`와 상관없이 앱이 뜨지 않습니다. 범위는 0~23입니다.
- `REPORT_ENABLED=true`인데 `DISCORD_BOT_TOKEN`이 비어 있으면 앱은 정상 기동하지만 21:00 전송이 매일 ERROR(`#errors`)로 실패한다. 설정 실수이니 둘을 함께 확인한다.
- 토큰은 서버 `.env`에만 둔다. 이 저장소·이슈·PR·로그에 남기지 않는다.
- `.env`를 수정한 뒤에는 컨테이너를 **재생성**해야 반영된다(`restart`만으로는 새 값이 들어가지 않는다).
- 봇 토큰이 유출되면 포털에서 즉시 재발급하고 `.env`를 갱신한다.

## 봇 준비

1. 디스코드 개발자 포털 → New Application → Bot 생성 → 토큰 복사.
2. OAuth2 URL(scope `bot`과 `applications.commands` 둘 다, 권한: View Channels · Send Messages · Embed Links)로 서버에 초대.
3. 포털에서 Privileged Gateway Intents는 켜지 않아도 된다(이 봇은 인텐트를 쓰지 않는다).
4. 디스코드 설정 → 고급 → 개발자 모드를 켠다. 그래야 아래 ID 복사가 보인다.
5. 리포트 채널을 만들고(팀원만 볼 수 있게 권한 설정) 우클릭 → 채널 ID 복사 → `DISCORD_REPORT_CHANNEL_ID`.
6. 서버 아이콘 우클릭 → 서버 ID 복사 → `DISCORD_GUILD_ID`.
7. 서버 설정 → 역할 → 운영진 역할 우클릭 → 역할 ID 복사 → `DISCORD_ADMIN_ROLE_ID`.

## 통계 API

```sh
curl -s -H "X-Stats-Token: $STATS_API_TOKEN" \
  "https://<도메인>/admin/stats?from=2026-10-27&to=2026-10-30"
```

- `from` 이상 `to` 미만(KST 자정 기준)의 숫자만 응답한다.
- `Authorization: Bearer`가 아니라 `X-Stats-Token` 헤더를 쓴다(JWT 필터와 충돌하기 때문).

## 동작 확인 / 문제 해결

- 마지막 전송 시각: `SELECT last_sent_at FROM report_settings;`
- 다시 보내고 싶으면 `UPDATE report_settings SET last_sent_at = NULL;` 후 다음 21:00을 기다린다(최초 실행은 최근 3일을 보낸다. 광고모드면 최근 1일).
- 전송이 두 번 연속 실패하면 ERROR 로그가 `#errors`로 가고 `last_sent_at`은 갱신되지 않아 다음 날 다시 시도한다.

## 슬래시 명령어

| 명령어 | 설명 | 응답 |
|---|---|---|
| `/help` | 등록된 명령어 목록(자동 생성) | 나만 보임 |
| `/가입자 [1·3·7·30일]` | 직전 N일 가입·탈퇴·누적(기본 7일) | 나만 보임 |
| `/리포트 지금` | 정기 리포트 즉시 생성(`last_sent_at` 변경 없음) | 채널 공개 |
| `/광고모드 켜기·끄기` | 리포트 주기 전환(매일 ↔ 3일), **운영진만** | 채널 공개 |
| `/광고모드 상태` | 현재 모드와 다음 리포트 날짜, **운영진만** | 나만 보임 |

- 명령어는 앱이 시작될 때 `DISCORD_GUILD_ID` 서버에만 등록된다. 코드에서 명령어를 바꾸면 재시작해야 반영된다.
- 광고모드·주기 변경은 다음 정기 실행(21:00)부터 반영된다.
- 명령어 등록 실패는 WARN 로그만 남고 `#errors`로 가지 않는다.
