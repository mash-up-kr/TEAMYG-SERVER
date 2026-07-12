# 브랜치 네이밍

> 브랜치 생성 전 읽을 것. PR 제목 포맷은 [`pull-request.md`](pull-request.md) 참조.

## 포맷

```
<type>/#<이슈번호>
```

- 이슈 없이 브랜치 생성 금지 — GitHub 이슈를 먼저 만들고 번호를 딴다.
- 작업명 suffix 없음 (`feat/#4-user-create` 형태 사용 안 함).

## type 목록

| type | 쓰임 |
|------|------|
| `feat` | 신규 기능 |
| `fix` | 버그 수정 |
| `refactor` | 리팩토링 |
| `chore` | 빌드·설정·CI 등 부수 작업 |
| `docs` | 문서 |
| `test` | 테스트 |

커밋 메시지 type과 동일하게 맞춘다 ([`commit-message.md`](commit-message.md) 참조).

## 예시

```
feat/#12
fix/#34
chore/#5
docs/#7
```

## 보호 브랜치

| 브랜치 | 용도 | 직접 push |
|--------|------|-----------|
| `main` | 배포 | 금지 — PR 必 |
| `develop` | 통합 개발 | 금지 — PR 必 |

- 모든 작업 브랜치는 `main`을 base로 분기한다. (당분간 `develop` 미사용)

## 머지 전략

**Merge Commit** — 히스토리를 그대로 보존한다. Squash·Rebase 머지 사용 안 함.