package parfait.http.parfait.controller

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import parfait.core.parfait.port.`in`.RotateParfaitCanvasesResult
import parfait.core.parfait.port.`in`.RotateParfaitCanvasesUseCase

/**
 * 컨텍스트 로딩만 필요한 테스트(actuator, openapi, security 화이트리스트 등)에서
 * `RotateParfaitCanvasesUseCase` 빈 부재로 인한 `NoSuchBeanDefinitionException`을 막기 위한 항상-존재 스텁이다.
 */
@TestConfiguration
class TestRotateParfaitCanvasesUseCaseConfig {
    @Bean
    fun rotateParfaitCanvasesUseCase(): RotateParfaitCanvasesUseCase =
        object : RotateParfaitCanvasesUseCase {
            override fun rotateAll(): RotateParfaitCanvasesResult =
                RotateParfaitCanvasesResult(closedCount = 0, emptyCount = 0, createdCount = 0, failedCount = 0)
        }
}
