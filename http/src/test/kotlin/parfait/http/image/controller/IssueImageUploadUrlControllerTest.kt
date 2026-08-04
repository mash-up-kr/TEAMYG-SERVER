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
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import parfait.core.exception.BusinessException
import parfait.core.image.exception.ImageErrorCode
import parfait.core.image.port.`in`.IssueImageUploadUrlResult
import parfait.core.image.port.`in`.IssueImageUploadUrlUseCase
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig

@WebMvcTest(controllers = [IssueImageUploadUrlController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalExceptionHandler::class,
    IssueImageUploadUrlControllerTest.UseCaseConfig::class,
    TestMemberQueryPortConfig::class,
    TestTokenValidatePortConfig::class,
)
class IssueImageUploadUrlControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var issueImageUploadUrlUseCase: IssueImageUploadUrlUseCase

    private val authentication = UsernamePasswordAuthenticationToken("42", null, emptyList())

    @Test
    fun `업로드 URL 발급에 성공하면 200과 imageId·uploadUrl·imageUrl·expiresIn을 응답한다`() {
        every { issueImageUploadUrlUseCase.issue(any()) } returns
            IssueImageUploadUrlResult(
                imageId = 77L,
                uploadUrl = "https://parfait-bucket.s3.ap-northeast-2.amazonaws.com/nukki/user42/key.png?sig",
                imageUrl = "https://parfait-bucket.s3.ap-northeast-2.amazonaws.com/nukki/user42/key.png",
                expiresIn = 900,
            )

        mockMvc
            .post("/api/v1/images") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"fileName":"photo.png","contentType":"image/png","imageType":"NUKKI"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.imageId") { value(77) }
                jsonPath("$.data.expiresIn") { value(900) }
            }

        verify {
            issueImageUploadUrlUseCase.issue(
                match { it.memberId == 42L && it.contentType == "image/png" },
            )
        }
    }

    @Test
    fun `imageType이 NUKKI·BACKGROUND가 아니면 400과 INVALID_REQUEST로 응답한다`() {
        mockMvc
            .post("/api/v1/images") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"fileName":"photo.png","contentType":"image/png","imageType":"INVALID"}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("INVALID_REQUEST") }
            }
    }

    @Test
    fun `지원하지 않는 contentType이면 400과 INVALID_CONTENT_TYPE으로 응답한다`() {
        every { issueImageUploadUrlUseCase.issue(any()) } throws
            BusinessException(ImageErrorCode.INVALID_CONTENT_TYPE)

        mockMvc
            .post("/api/v1/images") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"fileName":"photo.gif","contentType":"image/gif","imageType":"NUKKI"}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("INVALID_CONTENT_TYPE") }
            }
    }

    @Test
    fun `존재하지 않는 회원이면 404와 MEMBER_NOT_FOUND로 응답한다`() {
        every { issueImageUploadUrlUseCase.issue(any()) } throws
            BusinessException(ImageErrorCode.MEMBER_NOT_FOUND)

        mockMvc
            .post("/api/v1/images") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"fileName":"photo.png","contentType":"image/png","imageType":"NUKKI"}"""
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("MEMBER_NOT_FOUND") }
            }
    }

    @TestConfiguration
    class UseCaseConfig {
        @Bean
        fun issueImageUploadUrlUseCase(): IssueImageUploadUrlUseCase = mockk()
    }
}
