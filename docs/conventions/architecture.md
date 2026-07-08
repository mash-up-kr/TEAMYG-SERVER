# 아키텍처

> 헥사고날 아키텍처 기반 멀티모듈 구조·의존 규칙. **모듈/기능/의존성 추가 작업 전 읽을 것.**
> 빌드·실행은 루트 `CLAUDE.md` 참조.

헥사고날 아키텍처(Ports & Adapters). 모듈은 **단일 Gradle 프로젝트 `parfait/`** 안의 레이어별 서브모듈이다. 의존성은 **항상 안쪽(core → common)으로** 향하고, import 경계는 각 모듈의 `build.gradle.kts` 의존성 선언이 강제한다.

```
bootstrap → http / persistence / external / batch → core → common
```

| 모듈 | 역할 | import 가능 |
|------|------|-------------|
| **bootstrap** | 애플리케이션 진입점(`@SpringBootApplication`)·Bean 조립 | 전부 |
| **http** | Controller, Request/Response DTO, ExceptionHandler | core, common |
| **persistence** | Entity, Repository, out-port 구현(Adapter) | core, common |
| **external** | 외부 API 클라이언트, out-port 구현(Adapter) | core, common |
| **batch** | Job, Tasklet | core, common |
| **core** | Domain·Service·Port(in/out)·Exception. **비즈니스 규칙의 유일한 집합소** | **common 만.** 외부 의존 0 |
| **common** | 공통 응답 형식, 예외 코드, 유틸. **순수 코드** | 시스템 라이브러리만 (외부 패키지 금지) |

## 규칙

- `core`는 `http`, `persistence`, `external`, `batch`를 모른다. 외부 연동이 필요하면 `core`의 out-port 인터페이스를 정의하고, 구현체는 `persistence`·`external`이 담당한다.
- `common`은 Spring 포함 외부 프레임워크 의존 금지 — `build.gradle.kts`에 외부 패키지 추가 금지.
- 새 API 엔드포인트 → `http/controller/` + `core`의 in-port(UseCase) 호출.
- 새 비즈니스 로직 → `core/service/` + `core/port/in·out/` 인터페이스 정의.
- 새 외부 연동 → `external/adapter/`가 `core`의 out-port를 구현.
- 새 DB 접근 → `persistence/adapter/`가 `core`의 out-port를 구현.

## Port & Adapter — Composition Root(bootstrap)

`bootstrap`이 의존성 그래프를 1회 조립한다. `core`의 in-port는 Service가 구현하고, out-port는 `persistence`·`external` Adapter가 구현한다. **전역 싱글톤·순환 의존 금지** — Spring Bean 등록은 `bootstrap`에서만.

```kotlin
// core — 포트 정의 (외부를 모름)
interface TokenRepository : LoadTokenPort, SaveTokenPort  // out-port

// persistence — 포트 구현 (core 만 import)
@Component
class TokenRepositoryAdapter(
    private val tokenJpaRepository: TokenJpaRepository,
) : TokenRepository {
    override fun findByUserId(userId: Long): Token? = ...
    override fun save(token: Token): Token = ...
}
```

## 빌드 설정

- 공통 설정(repositories, java toolchain, kotlin-reflect, compilerOptions)은 루트 `build.gradle.kts`의 `subprojects` 블록으로 관리한다.
- `bootstrap`만 `bootJar`가 활성화되고 나머지 모듈은 일반 jar(`jar { enabled = true }`)로 빌드된다.
- `persistence`, `batch` 등 구현이 필요한 모듈은 의존성만 준비된 상태이며, 기능 구현 시 채운다.