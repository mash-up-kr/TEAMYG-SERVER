package parfait.http.global.security

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import parfait.core.auth.domain.LoginProvider
import parfait.core.auth.port.out.AccessTokenClaims
import parfait.core.auth.port.out.RefreshTokenClaims
import parfait.core.auth.port.out.RegistrationTokenClaims
import parfait.core.auth.port.out.TokenValidatePort

/**
 * `@WebMvcTest` 슬라이스 테스트는 `JwtAuthFilter`(Filter 타입)는 자동으로 슬라이스에 포함시키지만
 * 일반 `@Component`인 `JwtTokenAdapter`는 포함시키지 않는다.
 *
 * 이 스텁은 [TokenValidatePort] 타입 빈 부재로 인한 `NoSuchBeanDefinitionException`을 막기 위한
 * 것으로, 실제 토큰 검증 로직을 검증하지 않는다. 전체 컴포넌트 스캔을 사용하는 테스트에는
 * 절대 import하지 말 것 — 실제 `JwtTokenAdapter`와 타입이 중복되어 `NoUniqueBeanDefinitionException`이 발생한다.
 */
@TestConfiguration
class TestTokenValidatePortConfig {
    @Bean
    fun tokenValidatePort(): TokenValidatePort =
        object : TokenValidatePort {
            override fun validateAccessToken(token: String): AccessTokenClaims = AccessTokenClaims(0L, null)

            override fun validateRefreshToken(token: String): RefreshTokenClaims = RefreshTokenClaims(0L, "")

            override fun validateRegistrationToken(token: String): RegistrationTokenClaims =
                RegistrationTokenClaims(LoginProvider.KAKAO, "")
        }
}
