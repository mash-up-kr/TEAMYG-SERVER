package parfait.http.api.deeplink.controller

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import parfait.core.deeplink.domain.Platform
import parfait.core.deeplink.port.`in`.ResolveStoreFallbackCommand
import parfait.core.deeplink.port.`in`.ResolveStoreFallbackUseCase
import parfait.core.deeplink.port.`in`.StoreFallbackResult

/**
 * `ResolveStoreFallbackUseCase`의 실제 구현체(`ResolveStoreFallbackService`)는 `core` 모듈에 있고,
 * `http`의 `TestApplication`은 `parfait.http` 패키지만 스캔하므로 컨텍스트에 존재하지 않는다.
 *
 * 컨텍스트 로딩만 필요한 테스트(actuator, security 화이트리스트 등)에서 빈 부재로 인한
 * `NoSuchBeanDefinitionException`을 막기 위한 항상-존재 스텁이다.
 */
@TestConfiguration
class TestResolveStoreFallbackUseCaseConfig {
    @Bean
    fun resolveStoreFallbackUseCase(): ResolveStoreFallbackUseCase =
        object : ResolveStoreFallbackUseCase {
            override fun resolve(command: ResolveStoreFallbackCommand): StoreFallbackResult =
                StoreFallbackResult(platform = Platform.OTHER, redirectUrl = null)
        }
}
