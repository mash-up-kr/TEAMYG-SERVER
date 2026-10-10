package parfait.http.parfait.controller

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import parfait.core.parfait.domain.ParfaitStatus
import parfait.core.parfait.port.`in`.GetTodayParfaitIfChangedCommand
import parfait.core.parfait.port.`in`.GetTodayParfaitIfChangedResult
import parfait.core.parfait.port.`in`.GetTodayParfaitIfChangedUseCase
import parfait.core.parfait.port.`in`.GetTodayParfaitResult
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig
import java.time.LocalDate

@WebMvcTest(controllers = [ParfaitV2Controller::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalExceptionHandler::class,
    ParfaitV2ControllerTest.UseCaseConfig::class,
    TestMemberQueryPortConfig::class,
    TestTokenValidatePortConfig::class,
)
class ParfaitV2ControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var getTodayParfaitIfChangedUseCase: GetTodayParfaitIfChangedUseCase

    private val authentication = UsernamePasswordAuthenticationToken("42", null, emptyList())
    private val etag = "W/\"p100-v42-g3-m42\""

    private val body =
        GetTodayParfaitResult(
            parfaitId = 100L,
            groupName = "파르페",
            date = LocalDate.of(2026, 10, 10),
            status = ParfaitStatus.ACTIVE,
            lastClosedDate = null,
            groupMembers = emptyList(),
            background = null,
            images = null,
        )

    @Test
    fun `변경이 있으면 200 과 본문, ETag, Cache-Control 헤더를 응답한다`() {
        every { getTodayParfaitIfChangedUseCase.get(any()) } returns GetTodayParfaitIfChangedResult.Modified(etag, body)

        mockMvc
            .get("/api/v2/groups/1/parfaits/today") { principal = authentication }
            .andExpect {
                status { isOk() }
                header { string("ETag", etag) }
                header { string("Cache-Control", "private, no-cache") }
                jsonPath("$.data.parfaitId") { value(100) }
            }
    }

    @Test
    fun `변경이 없으면 304 와 헤더만 응답하고 본문은 비어 있다`() {
        every { getTodayParfaitIfChangedUseCase.get(any()) } returns GetTodayParfaitIfChangedResult.NotModified(etag)

        mockMvc
            .get("/api/v2/groups/1/parfaits/today") {
                principal = authentication
                header("If-None-Match", etag)
            }.andExpect {
                status { isNotModified() }
                header { string("ETag", etag) }
                header { string("Cache-Control", "private, no-cache") }
                content { string("") }
            }
    }

    @Test
    fun `If-None-Match 헤더와 경로·토큰의 값을 유스케이스로 전달한다`() {
        every { getTodayParfaitIfChangedUseCase.get(any()) } returns GetTodayParfaitIfChangedResult.NotModified(etag)

        mockMvc.get("/api/v2/groups/7/parfaits/today") {
            principal = authentication
            header("If-None-Match", etag)
        }

        verify {
            getTodayParfaitIfChangedUseCase.get(
                GetTodayParfaitIfChangedCommand(memberId = 42L, groupId = 7L, ifNoneMatch = etag),
            )
        }
    }

    @Test
    fun `헤더가 없으면 ifNoneMatch 는 null 로 전달한다`() {
        every { getTodayParfaitIfChangedUseCase.get(any()) } returns GetTodayParfaitIfChangedResult.Modified(etag, body)

        mockMvc.get("/api/v2/groups/1/parfaits/today") { principal = authentication }

        verify {
            getTodayParfaitIfChangedUseCase.get(
                GetTodayParfaitIfChangedCommand(memberId = 42L, groupId = 1L, ifNoneMatch = null),
            )
        }
    }

    @Test
    fun `그룹 멤버가 아니면 ETag 없이 에러를 응답한다`() {
        every { getTodayParfaitIfChangedUseCase.get(any()) } throws
            ParfaitGroupException(ParfaitGroupError.GROUP_NOT_JOINED)

        mockMvc
            .get("/api/v2/groups/1/parfaits/today") {
                principal = authentication
                header("If-None-Match", etag)
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.code") { value("GROUP_NOT_JOINED") }
                header { doesNotExist("ETag") }
            }
    }

    @TestConfiguration
    class UseCaseConfig {
        @Bean
        fun getTodayParfaitIfChangedUseCase(): GetTodayParfaitIfChangedUseCase = mockk()
    }
}
