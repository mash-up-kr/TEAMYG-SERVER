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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import parfait.core.exception.BusinessException
import parfait.core.notice.domain.Notice
import parfait.core.notice.domain.NoticePlatform
import parfait.core.notice.domain.NoticeType
import parfait.core.notice.exception.NoticeErrorCode
import parfait.core.notice.port.`in`.GetNoticesUseCase
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(controllers = [NoticeController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalExceptionHandler::class,
    NoticeControllerTest.UseCaseConfig::class,
    TestMemberQueryPortConfig::class,
    TestTokenValidatePortConfig::class,
)
class NoticeControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var getNoticesUseCase: GetNoticesUseCase

    @BeforeEach
    fun resetMocks() {
        clearMocks(getNoticesUseCase)
    }

    @Test
    fun `platform 으로 UseCase 를 호출하고 공지 목록을 돌려준다`() {
        every { getNoticesUseCase.getNotices(NoticePlatform.IOS) } returns
            listOf(
                Notice(
                    type = NoticeType.UPDATE,
                    platform = NoticePlatform.IOS,
                    version = "1.2.0",
                    revisionTarget = null,
                    title = "파르페 v1.2.0 업데이트 안내",
                    content = "내용",
                    publishedDate = LocalDate.of(2026, 11, 9),
                    createdAt = LocalDateTime.of(2026, 11, 9, 10, 0),
                    id = 7L,
                ),
            )

        mockMvc.get("/api/v1/notices?platform=IOS").andExpect {
            status { isOk() }
            jsonPath("$.data[0].id") { value(7) }
            jsonPath("$.data[0].type") { value("UPDATE") }
            jsonPath("$.data[0].title") { value("파르페 v1.2.0 업데이트 안내") }
            jsonPath("$.data[0].content") { value("내용") }
            jsonPath("$.data[0].publishedDate") { value("2026-11-09") }
        }

        verify(exactly = 1) { getNoticesUseCase.getNotices(NoticePlatform.IOS) }
    }

    @Test
    fun `platform 이 COMMON 이면 UseCase 의 예외가 400으로 변환된다`() {
        every { getNoticesUseCase.getNotices(NoticePlatform.COMMON) } throws
            BusinessException(NoticeErrorCode.INVALID_NOTICE_PLATFORM)

        mockMvc.get("/api/v1/notices?platform=COMMON").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_NOTICE_PLATFORM") }
        }
    }

    @Test
    fun `platform 이 없으면 400이다`() {
        mockMvc.get("/api/v1/notices").andExpect { status { isBadRequest() } }
    }

    @Test
    fun `알 수 없는 platform 이면 400이다`() {
        mockMvc.get("/api/v1/notices?platform=WINDOWS").andExpect { status { isBadRequest() } }
    }

    @TestConfiguration
    class UseCaseConfig {
        @Bean
        fun getNoticesUseCase(): GetNoticesUseCase = mockk()
    }
}
