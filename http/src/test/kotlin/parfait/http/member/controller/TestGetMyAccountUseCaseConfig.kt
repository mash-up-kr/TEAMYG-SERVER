package parfait.http.member.controller

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import parfait.core.auth.domain.LoginProvider
import parfait.core.member.port.`in`.GetMyAccountUseCase
import parfait.core.member.port.`in`.MyAccountResult

/**
 * `GetMyAccountUseCase`의 실제 구현체(`MemberService`)는 `core` 모듈에 있고,
 * `http`의 `TestApplication`은 `parfait.http` 패키지만 스캔하므로 컨텍스트에 존재하지 않는다.
 *
 * 컨텍스트 로딩만 필요한 테스트(actuator, openapi, security 화이트리스트 등)에서 빈 부재로 인한
 * `NoSuchBeanDefinitionException`을 막기 위한 항상-존재 스텁이다.
 */
@TestConfiguration
class TestGetMyAccountUseCaseConfig {
    @Bean
    fun getMyAccountUseCase(): GetMyAccountUseCase =
        object : GetMyAccountUseCase {
            override fun getMyAccount(memberId: Long): MyAccountResult =
                MyAccountResult(memberId, LoginProvider.KAKAO, "stub-nickname")
        }
}
