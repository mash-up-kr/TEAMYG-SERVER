package parfait.http.parfait.controller

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
import parfait.core.parfait.domain.OwnerType
import parfait.core.parfait.domain.ParfaitStatus
import parfait.core.parfait.port.`in`.GetTodayParfaitIfChangedCommand
import parfait.core.parfait.port.`in`.GetTodayParfaitIfChangedResult
import parfait.core.parfait.port.`in`.GetTodayParfaitIfChangedUseCase
import parfait.core.parfait.port.`in`.GetTodayParfaitResult
import parfait.core.parfait.port.`in`.PlacedByResult
import parfait.core.parfait.port.`in`.TodayParfaitImageResult
import parfait.core.parfait.port.`in`.TodayParfaitTextResult
import parfait.core.parfaitgroup.domain.NameTagChipType
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException
import parfait.core.parfaitimage.domain.BorderType
import parfait.core.parfaittext.domain.TextAlign
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig
import java.time.LocalDate
import java.time.LocalDateTime

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
    fun `이미지에는 IMAGE 와 주소를, 텍스트에는 TEXT 와 null 주소를 응답한다`() {
        val placedBy = PlacedByResult(10L, "연경이", NameTagChipType.TYPE12, OwnerType.ME)
        val createdAt = LocalDateTime.of(2026, 10, 10, 14, 30)
        val withToppings =
            body.copy(
                images =
                    listOf(
                        TodayParfaitImageResult(
                            parfaitImageId = 201L,
                            imageId = 77L,
                            imageUrl = "https://cdn.example.com/nukki/a.png",
                            positionX = 120.5,
                            positionY = 340.2,
                            positionZ = 1,
                            scale = 1.0,
                            rotation = 0.0,
                            borderType = BorderType.SOLID,
                            borderColor = "#000000",
                            borderWidth = 2.0,
                            placedBy = placedBy,
                            createdAt = createdAt,
                        ),
                    ),
                texts =
                    listOf(
                        TodayParfaitTextResult(
                            parfaitTextId = 301L,
                            text = "오늘도 행복한 하루!",
                            positionX = 10.0,
                            positionY = 20.0,
                            positionZ = 2,
                            scale = 1.5,
                            rotation = -30.0,
                            color = "#FF5733",
                            fontSize = 16.0,
                            isFill = false,
                            isStroke = true,
                            textAlign = TextAlign.RIGHT,
                            placedBy = placedBy,
                            createdAt = createdAt,
                        ),
                    ),
            )
        every { getTodayParfaitIfChangedUseCase.get(any()) } returns
            GetTodayParfaitIfChangedResult.Modified(etag, withToppings)

        mockMvc
            .get("/api/v2/groups/1/parfaits/today") { principal = authentication }
            .andExpect {
                status { isOk() }
                jsonPath("$.data.images[0].toppingType") { value("IMAGE") }
                jsonPath("$.data.images[0].parfaitImageId") { value(201) }
                jsonPath("$.data.images[0].imageUrl") { value("https://cdn.example.com/nukki/a.png") }
                jsonPath("$.data.images[0].borderType") { value("SOLID") }
                jsonPath("$.data.texts[0].toppingType") { value("TEXT") }
                jsonPath("$.data.texts[0].parfaitTextId") { value(301) }
                jsonPath("$.data.texts[0].imageUrl") { value(nullValue()) }
                jsonPath("$.data.texts[0].text") { value("오늘도 행복한 하루!") }
                jsonPath("$.data.texts[0].isFill") { value(false) }
                jsonPath("$.data.texts[0].isStroke") { value(true) }
                jsonPath("$.data.texts[0].textAlign") { value("RIGHT") }
                jsonPath("$.data.texts[0].placedBy.ownerType") { value("ME") }
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
