package parfait.http.image.controller

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import parfait.core.image.domain.ImageStatus
import parfait.core.image.port.`in`.ConfirmImageUploadCommand
import parfait.core.image.port.`in`.ConfirmImageUploadResult
import parfait.core.image.port.`in`.ConfirmImageUploadUseCase

/**
 * 컨텍스트 로딩만 필요한 테스트(actuator, openapi, security 화이트리스트 등)에서
 * `ConfirmImageUploadUseCase` 빈 부재로 인한 `NoSuchBeanDefinitionException`을 막기 위한 항상-존재 스텁이다.
 */
@TestConfiguration
class TestConfirmImageUploadUseCaseConfig {
    @Bean
    fun confirmImageUploadUseCase(): ConfirmImageUploadUseCase =
        object : ConfirmImageUploadUseCase {
            override fun confirm(command: ConfirmImageUploadCommand): ConfirmImageUploadResult =
                ConfirmImageUploadResult(
                    imageId = command.imageId,
                    imageUrl = "stub-image-url",
                    status = ImageStatus.COMPLETED,
                )
        }
}
