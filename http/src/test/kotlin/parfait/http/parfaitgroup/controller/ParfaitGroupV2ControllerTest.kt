package parfait.http.parfaitgroup.controller

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import parfait.core.parfait.domain.ToppingType
import parfait.core.parfaitgroup.application.port.`in`.GetMyParfaitGroupsUseCase
import parfait.core.parfaitgroup.application.port.`in`.MyParfaitGroupResult
import parfait.core.parfaitgroup.domain.NameTagChipType
import parfait.core.parfaitimage.domain.BorderType
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig
import java.time.LocalDateTime

@WebMvcTest(controllers = [ParfaitGroupV2Controller::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalExceptionHandler::class,
    ParfaitGroupV2ControllerTest.UseCaseConfig::class,
    TestMemberQueryPortConfig::class,
    TestTokenValidatePortConfig::class,
)
class ParfaitGroupV2ControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var getMyGroupsUseCase: GetMyParfaitGroupsUseCase

    private val authentication = UsernamePasswordAuthenticationToken("42", null, emptyList())

    @Test
    fun `최근 토핑이 이미지면 IMAGE 와 주소를, 텍스트면 TEXT 와 null 주소를 반환한다`() {
        every { getMyGroupsUseCase.getAllIncludingTexts(42L) } returns
            listOf(
                MyParfaitGroupResult(
                    groupId = 2L,
                    groupName = "텍스트 그룹",
                    recentImageUrl = null,
                    recentImageBorderType = null,
                    recentImageBorderColor = null,
                    recentImageBorderWidth = null,
                    recentImageUploadedAt = LocalDateTime.of(2026, 8, 1, 13, 0),
                    lastPlacedByNametagChip = NameTagChipType.TYPE7,
                    recentToppingType = ToppingType.TEXT,
                ),
                MyParfaitGroupResult(
                    groupId = 1L,
                    groupName = "이미지 그룹",
                    recentImageUrl = "https://image.example/latest",
                    recentImageBorderType = BorderType.SOLID,
                    recentImageBorderColor = "#FFD54F",
                    recentImageBorderWidth = 2.0,
                    recentImageUploadedAt = LocalDateTime.of(2026, 8, 1, 12, 0),
                    lastPlacedByNametagChip = NameTagChipType.TYPE3,
                    recentToppingType = ToppingType.IMAGE,
                ),
            )

        mockMvc
            .get("/api/v2/parfait-groups") {
                principal = authentication
            }.andExpect {
                status { isOk() }
                jsonPath("$.data[0].groupId") { value(2) }
                jsonPath("$.data[0].recentToppingType") { value("TEXT") }
                jsonPath("$.data[0].recentImageUrl") { value(nullValue()) }
                jsonPath("$.data[0].recentImageBorderType") { value(nullValue()) }
                jsonPath("$.data[0].recentImageUploadedAt") { value("2026-08-01T13:00:00") }
                jsonPath("$.data[0].lastPlacedByNameTagChip") { value("TYPE7") }
                jsonPath("$.data[1].groupId") { value(1) }
                jsonPath("$.data[1].recentToppingType") { value("IMAGE") }
                jsonPath("$.data[1].recentImageUrl") { value("https://image.example/latest") }
                jsonPath("$.data[1].recentImageBorderType") { value("SOLID") }
            }
        verify { getMyGroupsUseCase.getAllIncludingTexts(42L) }
    }

    @Test
    fun `오늘 토핑이 없으면 recentToppingType 과 recentImageUrl 이 모두 null 이다`() {
        every { getMyGroupsUseCase.getAllIncludingTexts(42L) } returns
            listOf(
                MyParfaitGroupResult(
                    groupId = 1L,
                    groupName = "빈 그룹",
                    recentImageUrl = null,
                    recentImageBorderType = null,
                    recentImageBorderColor = null,
                    recentImageBorderWidth = null,
                    recentImageUploadedAt = LocalDateTime.of(2026, 8, 1, 0, 0),
                    lastPlacedByNametagChip = NameTagChipType.TYPE3,
                ),
            )

        mockMvc
            .get("/api/v2/parfait-groups") {
                principal = authentication
            }.andExpect {
                status { isOk() }
                jsonPath("$.data[0].recentToppingType") { value(nullValue()) }
                jsonPath("$.data[0].recentImageUrl") { value(nullValue()) }
            }
    }

    @TestConfiguration
    class UseCaseConfig {
        @Bean
        fun getMyParfaitGroupsUseCase(): GetMyParfaitGroupsUseCase = mockk()
    }
}
