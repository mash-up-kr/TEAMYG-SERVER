package parfait.http.auth.controller

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import parfait.core.auth.port.`in`.AppleLoginResult
import parfait.core.auth.port.`in`.AppleLoginUseCase

/**
 * `AppleLoginUseCase`의 실제 구현체(`AppleLoginService`)는 `core` 모듈에 있고,
 * `http`의 `TestApplication`은 `parfait.http` 패키지만 스캔하므로 컨텍스트에 존재하지 않는다.
 *
 * 컨텍스트 로딩만 필요한 테스트(actuator, openapi, security 화이트리스트 등)에서 빈 부재로 인한
 * `NoSuchBeanDefinitionException`을 막기 위한 항상-존재 스텁이다.
 * 실제 로그인 로직 검증 목적이 아니므로 무조건 신규 유저 결과를 반환한다.
 */
@TestConfiguration
class TestAppleLoginUseCaseConfig {
    @Bean
    fun appleLoginUseCase(): AppleLoginUseCase =
        object : AppleLoginUseCase {
            override fun login(
                identityToken: String,
                nonce: String,
                authorizationCode: String,
            ): AppleLoginResult = AppleLoginResult.NewUser("stub-registration-token")
        }
}
