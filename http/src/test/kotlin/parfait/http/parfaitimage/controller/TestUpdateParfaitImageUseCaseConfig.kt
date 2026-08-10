package parfait.http.parfaitimage.controller

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageCommand
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageResult
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageUseCase

/**
 * 컨텍스트 로딩만 필요한 테스트(actuator, openapi, security 화이트리스트 등)에서
 * `UpdateParfaitImageUseCase` 빈 부재로 인한 `NoSuchBeanDefinitionException`을 막기 위한 항상-존재 스텁이다.
 */
@TestConfiguration
class TestUpdateParfaitImageUseCaseConfig {
    @Bean
    fun updateParfaitImageUseCase(): UpdateParfaitImageUseCase =
        object : UpdateParfaitImageUseCase {
            override fun update(command: UpdateParfaitImageCommand): UpdateParfaitImageResult =
                UpdateParfaitImageResult(
                    parfaitImageId = command.parfaitImageId,
                    positionX = command.positionX ?: 0.0,
                    positionY = command.positionY ?: 0.0,
                    positionZ = command.positionZ ?: 0,
                    scale = command.scale ?: 1.0,
                    rotation = command.rotation ?: 0.0,
                )
        }
}
