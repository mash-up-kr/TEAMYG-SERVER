package parfait.http.parfaittext.controller

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.verify
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.patch
import parfait.core.exception.BusinessException
import parfait.core.parfait.exception.ParfaitErrorCode
import parfait.core.parfaittext.domain.TextAlign
import parfait.core.parfaittext.exception.ParfaitTextErrorCode
import parfait.core.parfaittext.port.`in`.DeleteParfaitTextCommand
import parfait.core.parfaittext.port.`in`.DeleteParfaitTextUseCase
import parfait.core.parfaittext.port.`in`.UpdateParfaitTextCommand
import parfait.core.parfaittext.port.`in`.UpdateParfaitTextResult
import parfait.core.parfaittext.port.`in`.UpdateParfaitTextUseCase
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig

@WebMvcTest(controllers = [UpdateParfaitTextController::class, DeleteParfaitTextController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalExceptionHandler::class,
    TestParfaitTextMutationUseCaseConfig::class,
    TestMemberQueryPortConfig::class,
    TestTokenValidatePortConfig::class,
)
class ParfaitTextMutationControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var updateUseCase: UpdateParfaitTextUseCase

    @Autowired
    private lateinit var deleteUseCase: DeleteParfaitTextUseCase

    private val authentication = UsernamePasswordAuthenticationToken("42", null, emptyList())
    private val url = "/api/v1/groups/1/parfaits/5/texts/301"
    private val result =
        UpdateParfaitTextResult(
            parfaitTextId = 301L,
            text = "원래 텍스트",
            positionX = 0.0,
            positionY = 10.0,
            positionZ = 3,
            scale = 2.0,
            rotation = 45.0,
            color = "#FFFFFF",
            fontSize = 24.0,
            isFill = false,
            isStroke = true,
            textAlign = TextAlign.LEFT,
        )

    @BeforeEach
    fun setUp() {
        clearMocks(updateUseCase, deleteUseCase)
        every { updateUseCase.update(any()) } returns result
        every { deleteUseCase.delete(any()) } returns Unit
    }

    @Test
    fun `수정에 성공하면 200과 변경 후 텍스트 및 스타일을 반환한다`() {
        mockMvc
            .patch(url) {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content =
                    """
                    {
                      "positionX": 0.0,
                      "positionY": 10.0,
                      "positionZ": 3,
                      "scale": 2.0,
                      "rotation": 45.0,
                      "color": "#FFFFFF",
                      "fontSize": 24.0,
                      "isFill": false,
                      "isStroke": true,
                      "textAlign": "LEFT"
                    }
                    """.trimIndent()
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.code") { value("OK") }
                jsonPath("$.data.parfaitTextId") { value(301) }
                jsonPath("$.data.text") { value("원래 텍스트") }
                jsonPath("$.data.positionX") { value(0.0) }
                jsonPath("$.data.positionY") { value(10.0) }
                jsonPath("$.data.positionZ") { value(3) }
                jsonPath("$.data.scale") { value(2.0) }
                jsonPath("$.data.rotation") { value(45.0) }
                jsonPath("$.data.color") { value("#FFFFFF") }
                jsonPath("$.data.fontSize") { value(24.0) }
                jsonPath("$.data.isFill") { value(false) }
                jsonPath("$.data.isStroke") { value(true) }
                jsonPath("$.data.textAlign") { value("LEFT") }
                jsonPath("$.data.fill") { doesNotExist() }
                jsonPath("$.data.stroke") { doesNotExist() }
            }
        verify {
            updateUseCase.update(
                UpdateParfaitTextCommand(
                    memberId = 42L,
                    groupId = 1L,
                    parfaitId = 5L,
                    parfaitTextId = 301L,
                    positionX = 0.0,
                    positionY = 10.0,
                    positionZ = 3,
                    scale = 2.0,
                    rotation = 45.0,
                    color = "#FFFFFF",
                    fontSize = 24.0,
                    isFill = false,
                    isStroke = true,
                    textAlign = TextAlign.LEFT,
                ),
            )
        }
    }

    @Test
    fun `부분 수정에서 생략하거나 null로 보낸 필드는 null command로 전달한다`() {
        mockMvc
            .patch(url) {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"positionX":null,"isFill":null,"isStroke":null,"textAlign":null}"""
            }.andExpect { status { isOk() } }

        verify {
            updateUseCase.update(UpdateParfaitTextCommand(42L, 1L, 5L, 301L))
        }
    }

    @Test
    fun `삭제에 성공하면 200과 null data를 반환한다`() {
        perform("DELETE").andExpect {
            status { isOk() }
            jsonPath("$.success") { value(true) }
            jsonPath("$.code") { value("OK") }
            jsonPath("$.data") { value(nullValue()) }
        }
        verify { deleteUseCase.delete(DeleteParfaitTextCommand(42L, 1L, 5L, 301L)) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["PATCH", "DELETE"])
    fun `변경 실패는 도메인 오류 코드와 상태로 응답한다`(method: String) {
        listOf(
            ParfaitTextErrorCode.PARFAIT_TEXT_NOT_FOUND,
            ParfaitTextErrorCode.PARFAIT_TEXT_NOT_OWNED,
            ParfaitErrorCode.PARFAIT_NOT_FOUND,
            ParfaitErrorCode.PARFAIT_ALREADY_CLOSED,
        ).forEach { error ->
            every { updateUseCase.update(any()) } throws BusinessException(error)
            every { deleteUseCase.delete(any()) } throws BusinessException(error)

            perform(method).andExpect {
                status { isEqualTo(error.status) }
                jsonPath("$.code") { value(error.code) }
            }
        }
    }

    @Test
    fun `잘못된 스타일은 400과 검증 오류 코드로 응답한다`() {
        listOf(ParfaitTextErrorCode.INVALID_TEXT_STYLE).forEach { error ->
            every { updateUseCase.update(any()) } throws BusinessException(error)
            perform("PATCH").andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value(error.code) }
            }
        }
    }

    @Test
    fun `수정 요청의 text 필드는 무시되어 수정 command에 전달되지 않는다`() {
        mockMvc
            .patch(url) {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"text":"바꾸면 안 되는 내용","isFill":true,"isStroke":false}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.text") { value("원래 텍스트") }
            }
        verify {
            updateUseCase.update(UpdateParfaitTextCommand(42L, 1L, 5L, 301L, isFill = true, isStroke = false))
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["""{"fontSize":"invalid"}""", """{"textAlign":"JUSTIFY"}"""])
    fun `필드 타입이 잘못되면 유스케이스 호출 없이 400을 반환한다`(content: String) {
        mockMvc
            .patch(url) {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                this.content = content
            }.andExpect { status { isBadRequest() } }
        verify(exactly = 0) { updateUseCase.update(any()) }
    }

    private fun perform(method: String): ResultActionsDsl =
        if (method == "PATCH") {
            mockMvc.patch(url) {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = "{}"
            }
        } else {
            mockMvc.delete(url) { principal = authentication }
        }
}
