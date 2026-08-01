package parfait.http.global.security

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import parfait.core.auth.domain.LoginProvider
import parfait.core.member.port.out.MemberQueryPort

/**
 * `JwtAuthFilter`의 생성자 의존성인 [MemberQueryPort]의 실제 구현체(`MemberAdapter`)는
 * `persistence` 모듈에 있다. `http` 모듈은 `persistence`에 의존하지 않으므로
 * `http`의 테스트 클래스패스에는 이 포트의 실제 구현체 빈이 존재하지 않는다.
 *
 * 컨텍스트 로딩만 필요한 테스트(actuator, openapi 등)에서 빈 부재로 인한
 * `NoSuchBeanDefinitionException`을 막기 위한 항상-존재 스텁이다.
 * 실제 인증 로직 검증 목적이 아니므로 무조건 true를 반환한다.
 */
@TestConfiguration
class TestMemberQueryPortConfig {
    @Bean
    fun memberQueryPort(): MemberQueryPort =
        object : MemberQueryPort {
            override fun existsById(memberId: Long): Boolean = true

            override fun findGlobalNicknameById(memberId: Long): String = "테스트"

            override fun findMemberIdByProvider(
                provider: LoginProvider,
                providerUserId: String,
            ): Long? = null
        }
}
