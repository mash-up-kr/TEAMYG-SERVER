package parfait.http.api.notification.controller

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import parfait.core.notification.port.`in`.RegisterDeviceTokenCommand
import parfait.core.notification.port.`in`.RegisterDeviceTokenUseCase

/**
 * `RegisterDeviceTokenUseCase`의 실제 구현체는 `core` 모듈에 있고, `http`의 `TestApplication`은
 * `parfait.http` 패키지만 스캔하므로 컨텍스트에 존재하지 않는다. 전체 컨텍스트를 띄우는 테스트에서
 * 빈 부재로 인한 `NoSuchBeanDefinitionException`을 막기 위한 스텁이다.
 */
@TestConfiguration
class TestDeviceTokenUseCaseConfig {
    @Bean
    fun registerDeviceTokenUseCase(): RegisterDeviceTokenUseCase =
        object : RegisterDeviceTokenUseCase {
            override fun register(command: RegisterDeviceTokenCommand) {}
        }
}
