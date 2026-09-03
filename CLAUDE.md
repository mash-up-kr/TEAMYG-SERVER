# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

Spring Boot(Kotlin) 기반 프로젝트. Gradle 멀티모듈 + 헥사고날 아키텍처(Ports & Adapters).

스택: Kotlin 2.2.21 / Spring Boot 4.0.6 / Java 21 toolchain / Gradle Kotlin DSL / JUnit5 / ktlint 1.5.0.

## 빌드·실행·검증

```sh
./gradlew build                # 전체 빌드 (CI는 build -x test → test → ktlintCheck 순으로 실행)
./gradlew test                 # 전체 테스트 (JUnit5)
./gradlew :bootstrap:bootRun   # 로컬 실행 — 진입점은 bootstrap 모듈
./gradlew ktlintCheck          # 린트 검사 (lefthook pre-commit에서도 *.kt/kts 대상 실행)
./gradlew ktlintFormat         # 린트 자동 수정
```

코드 변경 후 커밋 전에 `ktlintCheck`와 관련 모듈 `test`를 통과시킨다.

## 작업 워크플로

이슈 → 브랜치 → 커밋 → PR 사이클은 커스텀 커맨드로 진행한다 (`.claude/commands/`).

| 커맨드 | 역할 |
|---|---|
| `/issue` | GitHub 이슈 생성 + `main`에서 작업 브랜치 분기 |
| `/done` | 현재 변경사항을 논리 단위로 쪼개 커밋 |
| `/pr` | 커밋 로그 기반 PR 본문 작성 + `gh pr create` |

## 문서 네비게이션

상세 규칙은 아래 문서를 상황에 맞춰 Read한다.

| 상황 | 참조할 문서 |
|---|---|
| 모듈/기능/의존성 추가 | `docs/conventions/architecture.md` |
| 브랜치 생성 | `docs/conventions/branch-naming.md` |
| 커밋 메시지 작성 | `docs/conventions/commit-message.md` |
| PR 생성, 본문 작성 | `docs/conventions/pull-request.md` |
| 운영 DB Flyway 전환 (1회성 절차) | `docs/operations/flyway-cutover.md` |
| 도메인 HTTPS·리버스 프록시 구성 | `docs/operations/https-setup.md` |

