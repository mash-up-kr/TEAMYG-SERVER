package parfait.http.auth.controller

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import parfait.core.auth.port.`in`.LogoutUseCase

/**
 * `LogoutUseCase`의 실제 구현체(`LogoutService`)는 `core` 모듈에 있고,
 * `http`의 `TestApplication`은 `parfait.http` 패키지만 스캔하므로 컨텍스트에 존재하지 않는다.
 *
 * 컨텍스트 로딩만 필요한 테스트(actuator, openapi, security 화이트리스트 등)에서 빈 부재로 인한
 * `NoSuchBeanDefinitionException`을 막기 위한 항상-존재 스텁이다.
 * 실제 로그아웃 로직 검증 목적이 아니므로 아무 동작도 하지 않는다.
 */
@TestConfiguration
class TestLogoutUseCaseConfig {
    @Bean
    fun logoutUseCase(): LogoutUseCase =
        object : LogoutUseCase {
            override fun logout(
                memberId: Long,
                refreshToken: String,
            ) {}
        }
}
