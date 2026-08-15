package parfait.http.parfait.controller

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import parfait.core.parfait.port.`in`.BackgroundResult
import parfait.core.parfait.port.`in`.ChangeParfaitBackgroundCommand
import parfait.core.parfait.port.`in`.ChangeParfaitBackgroundUseCase

/**
 * 컨텍스트 로딩만 필요한 테스트(actuator, openapi, security 화이트리스트 등)에서
 * `ChangeParfaitBackgroundUseCase` 빈 부재로 인한 `NoSuchBeanDefinitionException`을 막기 위한 항상-존재 스텁이다.
 */
@TestConfiguration
class TestChangeParfaitBackgroundUseCaseConfig {
    @Bean
    fun changeParfaitBackgroundUseCase(): ChangeParfaitBackgroundUseCase =
        object : ChangeParfaitBackgroundUseCase {
            override fun change(command: ChangeParfaitBackgroundCommand): BackgroundResult =
                throw UnsupportedOperationException("stub")
        }
}
