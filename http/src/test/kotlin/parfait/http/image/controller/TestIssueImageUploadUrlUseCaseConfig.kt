package parfait.http.image.controller

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import parfait.core.image.port.`in`.IssueImageUploadUrlCommand
import parfait.core.image.port.`in`.IssueImageUploadUrlResult
import parfait.core.image.port.`in`.IssueImageUploadUrlUseCase

/**
 * 컨텍스트 로딩만 필요한 테스트(actuator, openapi, security 화이트리스트 등)에서
 * `IssueImageUploadUrlUseCase` 빈 부재로 인한 `NoSuchBeanDefinitionException`을 막기 위한 항상-존재 스텁이다.
 */
@TestConfiguration
class TestIssueImageUploadUrlUseCaseConfig {
    @Bean
    fun issueImageUploadUrlUseCase(): IssueImageUploadUrlUseCase =
        object : IssueImageUploadUrlUseCase {
            override fun issue(command: IssueImageUploadUrlCommand): IssueImageUploadUrlResult =
                IssueImageUploadUrlResult(
                    imageId = 0L,
                    uploadUrl = "stub-upload-url",
                    imageUrl = "stub-image-url",
                    expiresIn = 900,
                )
        }
}
