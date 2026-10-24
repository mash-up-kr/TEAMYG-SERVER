package parfait.http.parfaittext.controller

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import parfait.core.exception.BusinessException
import parfait.core.parfait.domain.OwnerType
import parfait.core.parfait.exception.ParfaitErrorCode
import parfait.core.parfait.port.`in`.PlacedByResult
import parfait.core.parfait.port.`in`.TodayParfaitTextResult
import parfait.core.parfaitgroup.domain.NameTagChipType
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException
import parfait.core.parfaittext.exception.ParfaitTextErrorCode
import parfait.core.parfaittext.port.`in`.PlaceParfaitTextUseCase
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig
import java.time.LocalDateTime

@WebMvcTest(controllers = [PlaceParfaitTextController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalExceptionHandler::class,
    PlaceParfaitTextControllerTest.UseCaseConfig::class,
    TestMemberQueryPortConfig::class,
    TestTokenValidatePortConfig::class,
)
class PlaceParfaitTextControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var useCase: PlaceParfaitTextUseCase

    private val authentication = UsernamePasswordAuthenticationToken("42", null, emptyList())

    @BeforeEach
    fun resetMock() {
        clearMocks(useCase)
    }

    private val body =
        """
        {
          "text": "안녕 파르페!",
          "positionX": 120.5,
          "positionY": 340.2,
          "positionZ": 2,
          "scale": 1.5,
          "rotation": -30.0,
          "color": "#FF5733",
          "fontSize": 16.0,
          "isFill": false,
          "isStroke": true
        }
        """.trimIndent()

    @Test
    fun `텍스트 배치에 성공하면 201과 텍스트 및 스타일을 응답한다`() {
        every { useCase.place(any()) } returns
            TodayParfaitTextResult(
                parfaitTextId = 301L,
                text = "안녕 파르페!",
                positionX = 120.5,
                positionY = 340.2,
                positionZ = 2,
                scale = 1.5,
                rotation = -30.0,
                color = "#FF5733",
                fontSize = 16.0,
                isFill = false,
                isStroke = true,
                placedBy = PlacedByResult(10L, "연경이", NameTagChipType.TYPE6, OwnerType.ME),
                createdAt = LocalDateTime.of(2026, 10, 20, 14, 30),
            )

        mockMvc
            .post("/api/v1/groups/1/parfaits/5/texts") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = body
            }.andExpect {
                status { isCreated() }
                jsonPath("$.data.parfaitTextId") { value(301) }
                jsonPath("$.data.text") { value("안녕 파르페!") }
                jsonPath("$.data.positionX") { value(120.5) }
                jsonPath("$.data.positionY") { value(340.2) }
                jsonPath("$.data.positionZ") { value(2) }
                jsonPath("$.data.scale") { value(1.5) }
                jsonPath("$.data.rotation") { value(-30.0) }
                jsonPath("$.data.color") { value("#FF5733") }
                jsonPath("$.data.fontSize") { value(16.0) }
                jsonPath("$.data.isFill") { value(false) }
                jsonPath("$.data.isStroke") { value(true) }
                jsonPath("$.data.placedBy.nameTagChip") { value("TYPE6") }
            }
        verify {
            useCase.place(
                match {
                    it.memberId == 42L &&
                        it.groupId == 1L &&
                        it.parfaitId == 5L &&
                        it.text == "안녕 파르페!" &&
                        it.color == "#FF5733" &&
                        it.fontSize == 16.0 &&
                        !it.isFill &&
                        it.isStroke &&
                        it.positionX == 120.5 &&
                        it.positionY == 340.2 &&
                        it.positionZ == 2 &&
                        it.scale == 1.5 &&
                        it.rotation == -30.0
                },
            )
        }
    }

    @Test
    fun `채움과 외곽선을 생략하면 기본값으로 저장한다`() {
        every { useCase.place(any()) } returns
            TodayParfaitTextResult(
                parfaitTextId = 301L,
                text = "기본 텍스트",
                positionX = 0.0,
                positionY = 0.0,
                positionZ = 1,
                scale = 1.0,
                rotation = 0.0,
                color = "#FFFFFF",
                fontSize = 16.0,
                placedBy = PlacedByResult(10L, "연경이", NameTagChipType.TYPE6, OwnerType.ME),
                createdAt = LocalDateTime.of(2026, 10, 20, 14, 30),
            )
        mockMvc
            .post("/api/v1/groups/1/parfaits/5/texts") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content =
                    """
                    {
                      "text": "기본 텍스트",
                      "positionX": 0.0,
                      "positionY": 0.0,
                      "positionZ": 1,
                      "scale": 1.0,
                      "rotation": 0.0,
                      "color": "#FFFFFF",
                      "fontSize": 16.0
                    }
                    """.trimIndent()
            }.andExpect {
                status { isCreated() }
                jsonPath("$.data.isFill") { value(false) }
                jsonPath("$.data.isStroke") { value(false) }
            }
        verify { useCase.place(match { !it.isFill && !it.isStroke }) }
    }

    @Test
    fun `도메인 오류의 상태와 코드를 응답한다`() {
        listOf(
            ParfaitErrorCode.PARFAIT_NOT_FOUND,
            ParfaitErrorCode.PARFAIT_ALREADY_CLOSED,
            ParfaitTextErrorCode.INVALID_TEXT,
            ParfaitTextErrorCode.INVALID_TEXT_STYLE,
        ).forEach { error ->
            every { useCase.place(any()) } throws BusinessException(error)
            mockMvc
                .post("/api/v1/groups/1/parfaits/5/texts") {
                    principal = authentication
                    contentType = MediaType.APPLICATION_JSON
                    content = body
                }.andExpect {
                    status { isEqualTo(error.status) }
                    jsonPath("$.code") { value(error.code) }
                }
        }
    }

    @Test
    fun `그룹 미참여자는 403으로 응답한다`() {
        every { useCase.place(any()) } throws ParfaitGroupException(ParfaitGroupError.GROUP_NOT_JOINED)
        mockMvc
            .post("/api/v1/groups/1/parfaits/5/texts") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = body
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.code") { value("GROUP_NOT_JOINED") }
            }
    }

    @Test
    fun `필수 필드가 빠지면 400으로 응답한다`() {
        mockMvc
            .post("/api/v1/groups/1/parfaits/5/texts") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"text":"안녕"}"""
            }.andExpect {
                status { isBadRequest() }
            }
        verify(exactly = 0) { useCase.place(any()) }
    }

    @TestConfiguration
    class UseCaseConfig {
        @Bean
        fun placeParfaitTextUseCase(): PlaceParfaitTextUseCase = mockk()
    }
}
