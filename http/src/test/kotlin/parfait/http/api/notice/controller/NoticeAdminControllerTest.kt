package parfait.http.api.notice.controller

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import parfait.core.exception.BusinessException
import parfait.core.notice.domain.Notice
import parfait.core.notice.domain.NoticePlatform
import parfait.core.notice.domain.NoticeRevisionTarget
import parfait.core.notice.domain.NoticeType
import parfait.core.notice.exception.NoticeErrorCode
import parfait.core.notice.port.`in`.RegisterNoticeCommand
import parfait.core.notice.port.`in`.RegisterNoticeUseCase
import parfait.core.notice.port.`in`.UpdateNoticeCommand
import parfait.core.notice.port.`in`.UpdateNoticeUseCase
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(controllers = [NoticeAdminController::class], properties = ["notice.admin-token=test-notice-token"])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalExceptionHandler::class,
    NoticeAdminTokenVerifier::class,
    NoticeAdminControllerTest.UseCaseConfig::class,
    TestMemberQueryPortConfig::class,
    TestTokenValidatePortConfig::class,
)
class NoticeAdminControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var registerNoticeUseCase: RegisterNoticeUseCase

    @Autowired
    private lateinit var updateNoticeUseCase: UpdateNoticeUseCase

    @BeforeEach
    fun resetMocks() {
        clearMocks(registerNoticeUseCase, updateNoticeUseCase)
    }

    private val updateNotice =
        Notice(
            type = NoticeType.UPDATE,
            platform = NoticePlatform.COMMON,
            version = "1.2.0",
            revisionTarget = null,
            title = "파르페 v1.2.0 업데이트 안내",
            content = "내용",
            publishedDate = LocalDate.of(2026, 11, 9),
            createdAt = LocalDateTime.of(2026, 11, 9, 10, 0),
            id = 1L,
        )

    private val registerBody =
        """{"type":"UPDATE","platform":"COMMON","version":"1.2.0","content":"내용","publishedDate":"2026-11-09"}"""

    @Test
    fun `올바른 토큰이면 요청값 그대로 등록하고 title 을 돌려준다`() {
        every { registerNoticeUseCase.register(any()) } returns updateNotice

        mockMvc
            .post("/admin/notices") {
                header("X-Notice-Token", "test-notice-token")
                contentType = MediaType.APPLICATION_JSON
                content = registerBody
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.id") { value(1) }
                jsonPath("$.data.title") { value("파르페 v1.2.0 업데이트 안내") }
                jsonPath("$.data.platform") { value("COMMON") }
            }

        verify(exactly = 1) {
            registerNoticeUseCase.register(
                RegisterNoticeCommand(
                    type = NoticeType.UPDATE,
                    platform = NoticePlatform.COMMON,
                    version = "1.2.0",
                    revisionTarget = null,
                    content = "내용",
                    publishedDate = LocalDate.of(2026, 11, 9),
                ),
            )
        }
    }

    @Test
    fun `토큰이 없으면 401이고 등록하지 않는다`() {
        mockMvc
            .post("/admin/notices") {
                contentType = MediaType.APPLICATION_JSON
                content = registerBody
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.code") { value("INVALID_NOTICE_TOKEN") }
            }

        verify(exactly = 0) { registerNoticeUseCase.register(any()) }
    }

    @Test
    fun `토큰이 틀리면 401이고 등록하지 않는다`() {
        mockMvc
            .post("/admin/notices") {
                header("X-Notice-Token", "wrong")
                contentType = MediaType.APPLICATION_JSON
                content = registerBody
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.code") { value("INVALID_NOTICE_TOKEN") }
            }

        verify(exactly = 0) { registerNoticeUseCase.register(any()) }
    }

    @Test
    fun `content 가 비어 있으면 400이다`() {
        mockMvc
            .post("/admin/notices") {
                header("X-Notice-Token", "test-notice-token")
                contentType = MediaType.APPLICATION_JSON
                content = """{"type":"UPDATE","platform":"COMMON","version":"1.2.0","content":" "}"""
            }.andExpect {
                status { isBadRequest() }
            }

        verify(exactly = 0) { registerNoticeUseCase.register(any()) }
    }

    @Test
    fun `수정하면 UpdateNoticeCommand 를 전달하고 결과를 돌려준다`() {
        every { updateNoticeUseCase.update(1L, any()) } returns updateNotice.copy(content = "새 내용")

        mockMvc
            .patch("/admin/notices/1") {
                header("X-Notice-Token", "test-notice-token")
                contentType = MediaType.APPLICATION_JSON
                content = """{"content":"새 내용","revisionTarget":"TERMS"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.content") { value("새 내용") }
            }

        verify(exactly = 1) {
            updateNoticeUseCase.update(
                1L,
                UpdateNoticeCommand(content = "새 내용", version = null, revisionTarget = NoticeRevisionTarget.TERMS),
            )
        }
    }

    @Test
    fun `수정 시 토큰이 없으면 401이고 수정하지 않는다`() {
        mockMvc
            .patch("/admin/notices/1") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"content":"새 내용"}"""
            }.andExpect {
                status { isUnauthorized() }
                jsonPath("$.code") { value("INVALID_NOTICE_TOKEN") }
            }

        verify(exactly = 0) { updateNoticeUseCase.update(any(), any()) }
    }

    @Test
    fun `수정 대상이 없으면 UseCase 의 예외가 404로 변환된다`() {
        every { updateNoticeUseCase.update(999L, any()) } throws BusinessException(NoticeErrorCode.NOTICE_NOT_FOUND)

        mockMvc
            .patch("/admin/notices/999") {
                header("X-Notice-Token", "test-notice-token")
                contentType = MediaType.APPLICATION_JSON
                content = """{"content":"새 내용"}"""
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("NOTICE_NOT_FOUND") }
            }
    }

    @TestConfiguration
    class UseCaseConfig {
        @Bean
        fun registerNoticeUseCase(): RegisterNoticeUseCase = mockk()

        @Bean
        fun updateNoticeUseCase(): UpdateNoticeUseCase = mockk()
    }
}
