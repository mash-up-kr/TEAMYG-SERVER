package parfait.http.image.controller

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import parfait.core.exception.BusinessException
import parfait.core.image.domain.ImageStatus
import parfait.core.image.exception.ImageErrorCode
import parfait.core.image.port.`in`.ConfirmImageUploadResult
import parfait.core.image.port.`in`.ConfirmImageUploadUseCase
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig

@WebMvcTest(controllers = [ConfirmImageUploadController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalExceptionHandler::class,
    ConfirmImageUploadControllerTest.UseCaseConfig::class,
    TestMemberQueryPortConfig::class,
    TestTokenValidatePortConfig::class,
)
class ConfirmImageUploadControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var confirmImageUploadUseCase: ConfirmImageUploadUseCase

    @Test
    fun `업로드 확인에 성공하면 200과 imageId·imageUrl·status를 응답한다`() {
        every { confirmImageUploadUseCase.confirm(any()) } returns
            ConfirmImageUploadResult(
                imageId = 77L,
                imageUrl = "https://parfait-bucket.s3.ap-northeast-2.amazonaws.com/nukki/user1/key.png",
                status = ImageStatus.COMPLETED,
            )

        mockMvc
            .post("/api/v1/images/77/confirm")
            .andExpect {
                status { isOk() }
                jsonPath("$.data.imageId") { value(77) }
                jsonPath("$.data.status") { value("COMPLETED") }
            }

        verify { confirmImageUploadUseCase.confirm(match { it.imageId == 77L }) }
    }

    @Test
    fun `존재하지 않는 imageId면 404와 IMAGE_NOT_FOUND로 응답한다`() {
        every { confirmImageUploadUseCase.confirm(any()) } throws
            BusinessException(ImageErrorCode.IMAGE_NOT_FOUND)

        mockMvc
            .post("/api/v1/images/999/confirm")
            .andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("IMAGE_NOT_FOUND") }
            }
    }

    @Test
    fun `이미 확인된 이미지면 409와 IMAGE_ALREADY_CONFIRMED로 응답한다`() {
        every { confirmImageUploadUseCase.confirm(any()) } throws
            BusinessException(ImageErrorCode.IMAGE_ALREADY_CONFIRMED)

        mockMvc
            .post("/api/v1/images/77/confirm")
            .andExpect {
                status { isConflict() }
                jsonPath("$.code") { value("IMAGE_ALREADY_CONFIRMED") }
            }
    }

    @TestConfiguration
    class UseCaseConfig {
        @Bean
        fun confirmImageUploadUseCase(): ConfirmImageUploadUseCase = mockk()
    }
}
