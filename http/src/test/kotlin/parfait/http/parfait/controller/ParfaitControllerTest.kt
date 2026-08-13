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
import parfait.core.exception.BusinessException
import parfait.core.parfait.exception.ParfaitErrorCode
import parfait.core.parfait.port.`in`.GetParfaitYearsUseCase
import parfait.core.parfait.port.`in`.GetPastParfaitsUseCase
import parfait.core.parfait.port.`in`.PastParfaitResult
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig
import java.time.LocalDate

@WebMvcTest(controllers = [ParfaitController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalExceptionHandler::class,
    ParfaitControllerTest.UseCaseConfig::class,
    TestMemberQueryPortConfig::class,
    TestTokenValidatePortConfig::class,
)
class ParfaitControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var getParfaitYearsUseCase: GetParfaitYearsUseCase

    @Autowired
    private lateinit var getPastParfaitsUseCase: GetPastParfaitsUseCase

    private val authentication = UsernamePasswordAuthenticationToken("42", null, emptyList())

    @Test
    fun `범위를 지정해 조회하면 200과 파르페 목록을 응답한다`() {
        every { getPastParfaitsUseCase.getPastParfaits(any()) } returns
            listOf(
                PastParfaitResult(
                    parfaitId = 98L,
                    date = LocalDate.of(2026, 7, 7),
                    thumbnailUrl = "https://parfait-bucket.s3.../completed/group1/2026-07-07.png",
                    imageCount = 4,
                ),
            )

        mockMvc
            .get("/api/v1/groups/1/parfaits") {
                principal = authentication
                param("from", "2026-07-01")
                param("to", "2026-07-31")
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.parfaits[0].parfaitId") { value(98) }
                jsonPath("$.data.parfaits[0].date") { value("2026-07-07") }
                jsonPath("$.data.parfaits[0].imageCount") { value(4) }
            }

        verify {
            getPastParfaitsUseCase.getPastParfaits(
                match {
                    it.memberId == 42L &&
                        it.groupId == 1L &&
                        it.from == LocalDate.of(2026, 7, 1) &&
                        it.to == LocalDate.of(2026, 7, 31)
                },
            )
        }
    }

    @Test
    fun `from과 to를 생략하면 null로 UseCase에 전달한다`() {
        every { getPastParfaitsUseCase.getPastParfaits(any()) } returns emptyList()

        mockMvc
            .get("/api/v1/groups/1/parfaits") {
                principal = authentication
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.parfaits") { isEmpty() }
            }

        verify {
            getPastParfaitsUseCase.getPastParfaits(
                match { it.from == null && it.to == null },
            )
        }
    }

    @Test
    fun `조회 시작일이 종료일보다 늦으면 400과 INVALID_DATE_RANGE로 응답한다`() {
        every { getPastParfaitsUseCase.getPastParfaits(any()) } throws
            BusinessException(ParfaitErrorCode.INVALID_DATE_RANGE)

        mockMvc
            .get("/api/v1/groups/1/parfaits") {
                principal = authentication
                param("from", "2026-08-01")
                param("to", "2026-07-01")
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("INVALID_DATE_RANGE") }
            }
    }

    @Test
    fun `그룹에 참여하지 않았으면 403과 GROUP_NOT_JOINED로 응답한다`() {
        every { getPastParfaitsUseCase.getPastParfaits(any()) } throws
            ParfaitGroupException(ParfaitGroupError.GROUP_NOT_JOINED)

        mockMvc
            .get("/api/v1/groups/1/parfaits") {
                principal = authentication
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.code") { value("GROUP_NOT_JOINED") }
            }
    }

    @TestConfiguration
    class UseCaseConfig {
        @Bean
        fun getParfaitYearsUseCase(): GetParfaitYearsUseCase = mockk()

        @Bean
        fun getPastParfaitsUseCase(): GetPastParfaitsUseCase = mockk()
    }
}
