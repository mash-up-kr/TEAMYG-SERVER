package parfait.http.parfaitimage.controller

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import parfait.core.parfaitimage.port.`in`.DeleteParfaitImageCommand
import parfait.core.parfaitimage.port.`in`.DeleteParfaitImageUseCase

/**
 * 컨텍스트 로딩만 필요한 테스트(actuator, openapi, security 화이트리스트 등)에서
 * `DeleteParfaitImageUseCase` 빈 부재로 인한 `NoSuchBeanDefinitionException`을 막기 위한 항상-존재 스텁이다.
 */
@TestConfiguration
class TestDeleteParfaitImageUseCaseConfig {
    @Bean
    fun deleteParfaitImageUseCase(): DeleteParfaitImageUseCase =
        object : DeleteParfaitImageUseCase {
            override fun delete(command: DeleteParfaitImageCommand) = Unit
        }
}
