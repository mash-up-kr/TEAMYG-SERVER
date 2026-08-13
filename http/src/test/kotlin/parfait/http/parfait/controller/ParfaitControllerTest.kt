package parfait.http.parfait.controller

import io.mockk.every
import io.mockk.mockk
import org.hamcrest.Matchers.nullValue
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
import parfait.core.parfait.domain.BackgroundType
import parfait.core.parfait.domain.ParfaitStatus
import parfait.core.parfait.port.`in`.BackgroundResult
import parfait.core.parfait.port.`in`.GetParfaitYearsUseCase
import parfait.core.parfait.port.`in`.GetTodayParfaitResult
import parfait.core.parfait.port.`in`.GetTodayParfaitUseCase
import parfait.core.parfait.port.`in`.GroupMemberResult
import parfait.core.parfait.port.`in`.PlacedByResult
import parfait.core.parfait.port.`in`.TodayParfaitImageResult
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException
import parfait.core.parfaitimage.domain.BorderType
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig
import java.time.LocalDate
import java.time.LocalDateTime

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
    private lateinit var getTodayParfaitUseCase: GetTodayParfaitUseCase

    private val authentication = UsernamePasswordAuthenticationToken("42", null, emptyList())

    @Test
    fun `오늘의 파르페 조회에 성공하면 200과 캔버스 정보를 응답한다`() {
        every { getTodayParfaitUseCase.get(any()) } returns
            GetTodayParfaitResult(
                parfaitId = 100L,
                date = LocalDate.of(2026, 7, 9),
                status = ParfaitStatus.ACTIVE,
                lastClosedDate = LocalDate.of(2026, 7, 8),
                groupMembers =
                    listOf(
                        GroupMemberResult(id = 10L, nickname = "연경이"),
                        GroupMemberResult(id = 11L, nickname = "서휘"),
                    ),
                background = BackgroundResult(type = BackgroundType.COLOR, value = "#FFFFFF"),
                images =
                    listOf(
                        TodayParfaitImageResult(
                            parfaitImageId = 201L,
                            imageId = 77L,
                            imageUrl = "https://parfait-bucket.s3.../nukki/user1/550e8400.png",
                            positionX = 120.5,
                            positionY = 340.2,
                            positionZ = 1,
                            scale = 1.0,
                            rotation = 0.0,
                            borderType = BorderType.SOLID,
                            borderColor = "#000000",
                            borderWidth = 2.0,
                            placedBy = PlacedByResult(groupMemberId = 10L, nickname = "연경이"),
                            createdAt = LocalDateTime.of(2026, 7, 9, 14, 30, 0),
                        ),
                    ),
            )

        mockMvc
            .get("/api/v1/groups/1/parfaits/today") {
                principal = authentication
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.parfaitId") { value(100) }
                jsonPath("$.data.status") { value("ACTIVE") }
                jsonPath("$.data.lastClosedDate") { value("2026-07-08") }
                jsonPath("$.data.groupMembers[0].nickname") { value("연경이") }
                jsonPath("$.data.background.type") { value("COLOR") }
                jsonPath("$.data.images[0].placedBy.nickname") { value("연경이") }
            }
    }

    @Test
    fun `빈 캔버스면 background와 images가 null로 응답된다`() {
        every { getTodayParfaitUseCase.get(any()) } returns
            GetTodayParfaitResult(
                parfaitId = 100L,
                date = LocalDate.of(2026, 7, 9),
                status = ParfaitStatus.ACTIVE,
                lastClosedDate = null,
                groupMembers = emptyList(),
                background = null,
                images = null,
            )

        mockMvc
            .get("/api/v1/groups/1/parfaits/today") {
                principal = authentication
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.background") { value(nullValue()) }
                jsonPath("$.data.images") { value(nullValue()) }
                jsonPath("$.data.lastClosedDate") { value(nullValue()) }
            }
    }

    @Test
    fun `그룹에 참여하지 않았으면 403과 GROUP_NOT_JOINED로 응답한다`() {
        every { getTodayParfaitUseCase.get(any()) } throws
            ParfaitGroupException(ParfaitGroupError.GROUP_NOT_JOINED)

        mockMvc
            .get("/api/v1/groups/1/parfaits/today") {
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
        fun getTodayParfaitUseCase(): GetTodayParfaitUseCase = mockk()
    }
}
