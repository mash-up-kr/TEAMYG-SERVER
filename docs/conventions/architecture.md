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
- **in-port 네이밍**: `core`의 in-port는 `{동작}UseCase` 형태로 짓는다(예: `LoginUseCase`, `RegisterMemberUseCase`). 보통 메서드 하나만 가져 "요청 하나"를 나타내며, `http`의 Controller는 이 인터페이스 타입만 안다 — 구현체인 Service 클래스 이름은 Controller 코드에 등장하지 않는다. in-port는 `core/service/`의 Service 클래스가 구현한다(예: `LoginService : LoginUseCase`). 한 Service가 여러 UseCase 인터페이스를 동시에 구현해도 된다 — in-port가 요청 종류별로 쪼개져 있다는 원칙과, 그걸 누가 구현하느냐는 별개의 문제다.
- **out-port 네이밍**: `core`에는 `Repository`라는 이름을 쓰지 않는다. `Repository`는 `persistence`의 Spring Data(JPA) 인터페이스 전용 이름이다. `core`의 out-port는 **도메인 이름을 앞, 동작을 뒤**에 두어 `{Domain}{동작}Port` 형태로 짓는다(예: `MemberQueryPort`, `MemberSavePort` — `QueryMemberPort`처럼 동작을 앞에 두지 않는다). 도메인을 앞에 두면 같은 도메인의 포트들이 파일 탐색기·IDE 자동완성에서 이름순으로 나란히 모인다.
- **out-port를 나누는 단위는 읽기/쓰기에 따라 다르다**:
  - **조회(Query) 메서드는 도메인당 포트 하나로 묶는다** — `{Domain}QueryPort`. 조회는 상태를 바꾸지 않으므로 여러 조회 메서드(`existsById`, `findByEmail` 등)가 한 포트에 있어도 오용 위험이 없다.
  - **쓰기(Command) 메서드는 액션별로 쪼갠다** — `{Domain}SavePort`, `{Domain}DeletePort` 등. `save`(생성/수정)와 `delete`(삭제)는 위험도가 다르고, 보통 하나의 유스케이스가 둘 다 필요로 하는 경우는 드물기 때문에, 소비자가 필요한 쓰기 능력만 정확히 갖도록 분리한다.
  - 이렇게 나누면 ① `persistence`의 기존 JPA 인터페이스 이름과 절대 겹치지 않아 리네이밍이 필요 없고, ② 포트를 쓰는 클래스의 생성자 시그니처만 봐도 "정확히 어떤 능력이 필요한지"가 드러나고, ③ 테스트에서 MockK로 목을 만들 때도 해당 포트에 정의된 메서드만 스텁하면 되어 간단해진다.

## Port & Adapter — Composition Root(bootstrap)

`bootstrap`이 의존성 그래프를 1회 조립한다. `core`의 in-port는 Service가 구현하고, out-port는 `persistence`·`external` Adapter가 구현한다. **전역 싱글톤·순환 의존 금지** — Spring Bean 등록은 `bootstrap`에서만.

```kotlin
// core — in-port 정의. 보통 메서드 하나짜리 "요청 하나"를 나타낸다
interface RegisterMemberUseCase {
    fun register(email: String): Member
}

// core — Service가 in-port를 구현한다. Controller는 구체 클래스(RegisterMemberService)가 아니라
// 인터페이스(RegisterMemberUseCase) 타입만 안다
@Service
class RegisterMemberService(
    private val memberSavePort: MemberSavePort,
) : RegisterMemberUseCase {
    override fun register(email: String): Member = memberSavePort.save(Member(email = email))
}

// core — 포트 정의. Query는 도메인당 하나로 묶고, Command는 액션별로 쪼갠다
interface MemberQueryPort {
    fun existsById(memberId: Long): Boolean
    fun findByEmail(email: String): Member?
}
interface MemberSavePort {
    fun save(member: Member): Member
}
interface MemberDeletePort {
    fun deleteById(memberId: Long)
}

// persistence — 어댑터가 필요한 포트들을 구현 (core 만 import)
// memberRepository는 기존 Spring Data JPA 인터페이스 그대로, 리네이밍 없음
@Component
class MemberAdapter(
    private val memberRepository: MemberRepository,
) : MemberQueryPort, MemberSavePort, MemberDeletePort {
    override fun existsById(memberId: Long) = memberRepository.existsById(memberId)
    override fun findByEmail(email: String): Member? = memberRepository.findByEmail(email)
    override fun save(member: Member): Member = memberRepository.save(member)
    override fun deleteById(memberId: Long) = memberRepository.deleteById(memberId)
}
```

## 빌드 설정

- 공통 설정(repositories, java toolchain, kotlin-reflect, compilerOptions)은 루트 `build.gradle.kts`의 `subprojects` 블록으로 관리한다.
- `bootstrap`만 `bootJar`가 활성화되고 나머지 모듈은 일반 jar(`jar { enabled = true }`)로 빌드된다.
- `persistence`, `batch` 등 구현이 필요한 모듈은 의존성만 준비된 상태이며, 기능 구현 시 채운다.