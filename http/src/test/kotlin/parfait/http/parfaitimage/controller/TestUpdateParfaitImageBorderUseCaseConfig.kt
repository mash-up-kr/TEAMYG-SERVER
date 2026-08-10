package parfait.http.parfaitimage.controller

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageBorderCommand
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageBorderResult
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageBorderUseCase

/**
 * 컨텍스트 로딩만 필요한 테스트(actuator, openapi, security 화이트리스트 등)에서
 * `UpdateParfaitImageBorderUseCase` 빈 부재로 인한 `NoSuchBeanDefinitionException`을 막기 위한 항상-존재 스텁이다.
 */
@TestConfiguration
class TestUpdateParfaitImageBorderUseCaseConfig {
    @Bean
    fun updateParfaitImageBorderUseCase(): UpdateParfaitImageBorderUseCase =
        object : UpdateParfaitImageBorderUseCase {
            override fun update(command: UpdateParfaitImageBorderCommand): UpdateParfaitImageBorderResult =
                UpdateParfaitImageBorderResult(
                    parfaitImageId = command.parfaitImageId,
                    borderType = command.borderType,
                    borderColor = command.borderColor,
                    borderWidth = command.borderWidth,
                )
        }
}
