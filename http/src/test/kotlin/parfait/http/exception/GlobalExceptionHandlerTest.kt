package parfait.http.exception

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import parfait.common.error.BaseErrorCode
import parfait.core.exception.BusinessException
import kotlin.test.Test

private object DummyErrorCode : BaseErrorCode {
    override val status = 400
    override val code = "DUMMY_ERROR"
    override val message = "더미 에러"
}

@WebMvcTest(controllers = [GlobalExceptionHandlerTest.DummyController::class])
@Import(GlobalExceptionHandler::class, GlobalExceptionHandlerTest.DummyController::class)
class GlobalExceptionHandlerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @RestController
    class DummyController {
        @GetMapping("/test/business-error")
        fun throwBusinessException(): Nothing = throw BusinessException(DummyErrorCode)

        @GetMapping("/test/unexpected-error")
        fun throwUnexpectedException(): Nothing = throw IllegalStateException("boom")

        @GetMapping("/test/missing-param")
        fun requireParam(
            @RequestParam name: String,
        ): String = name

        @PostMapping("/test/malformed-body")
        fun malformedBody(
            @RequestBody body: Map<String, Any>,
        ): Map<String, Any> = body

        @GetMapping("/test/type-mismatch")
        fun typeMismatch(
            @RequestParam id: Long,
        ): Long = id
    }

    @Test
    fun `BusinessException을 던지면 errorCode에 정의된 상태코드와 code로 응답한다`() {
        mockMvc.get("/test/business-error").andExpect {
            status { isBadRequest() }
            jsonPath("$.success") { value(false) }
            jsonPath("$.code") { value("DUMMY_ERROR") }
        }
    }

    @Test
    fun `예상 못한 예외를 던지면 500과 INTERNAL_SERVER_ERROR로 응답한다`() {
        mockMvc.get("/test/unexpected-error").andExpect {
            status { isInternalServerError() }
            jsonPath("$.success") { value(false) }
            jsonPath("$.code") { value("INTERNAL_SERVER_ERROR") }
        }
    }

    @Test
    fun `필수 파라미터가 없으면 400과 INVALID_REQUEST로 응답한다`() {
        mockMvc.get("/test/missing-param").andExpect {
            status { isBadRequest() }
            jsonPath("$.success") { value(false) }
            jsonPath("$.code") { value("INVALID_REQUEST") }
        }
    }

    @Test
    fun `요청 본문이 올바른 형식이 아니면 400과 INVALID_REQUEST로 응답한다`() {
        mockMvc
            .post("/test/malformed-body") {
                contentType = MediaType.APPLICATION_JSON
                content = "{ invalid json"
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.success") { value(false) }
                jsonPath("$.code") { value("INVALID_REQUEST") }
            }
    }

    @Test
    fun `파라미터 타입이 안 맞으면 400과 INVALID_REQUEST로 응답한다`() {
        mockMvc
            .get("/test/type-mismatch") {
                param("id", "not-a-number")
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.success") { value(false) }
                jsonPath("$.code") { value("INVALID_REQUEST") }
            }
    }
}
