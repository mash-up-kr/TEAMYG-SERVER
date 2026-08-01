package parfait.http.global.exception

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
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
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig
import kotlin.test.Test

data class ValidationTestRequest(
    @field:jakarta.validation.constraints.NotBlank val name: String,
)

private object DummyErrorCode : BaseErrorCode {
    override val status = 400
    override val code = "DUMMY_ERROR"
    override val message = "더미 에러"
}

// JwtAuthFilter는 Filter 타입이라 @WebMvcTest 슬라이스에 자동 포함되므로, 생성자 의존성
// 빈(TokenValidatePort, MemberQueryPort)을 스텁으로 채워 컨텍스트 로딩만 성공시킨다.
// 이 테스트의 목적은 GlobalExceptionHandler 검증이라 인증 필터 자체는 실제로
// 요청을 가로채지 않도록 addFilters = false로 비활성화한다.
@WebMvcTest(controllers = [GlobalExceptionHandlerTest.DummyController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalExceptionHandler::class,
    GlobalExceptionHandlerTest.DummyController::class,
    TestMemberQueryPortConfig::class,
    TestTokenValidatePortConfig::class,
)
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

        @PostMapping("/test/valid-body")
        fun validBody(
            @jakarta.validation.Valid @RequestBody body: ValidationTestRequest,
        ): ValidationTestRequest = body

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

    @Test
    fun `Bean Validation에 실패하면 400과 INVALID_REQUEST로 응답한다`() {
        mockMvc
            .post("/test/valid-body") {
                contentType = MediaType.APPLICATION_JSON
                content = """{"name":""}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.success") { value(false) }
                jsonPath("$.code") { value("INVALID_REQUEST") }
            }
    }
}
