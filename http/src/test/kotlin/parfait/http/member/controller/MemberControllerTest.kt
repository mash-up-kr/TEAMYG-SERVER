package parfait.http.member.controller

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
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.patch
import parfait.core.exception.BusinessException
import parfait.core.member.exception.MemberErrorCode
import parfait.core.member.port.`in`.ChangeGlobalNicknameResult
import parfait.core.member.port.`in`.ChangeGlobalNicknameUseCase
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig

@WebMvcTest(controllers = [MemberController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalExceptionHandler::class,
    MemberControllerTest.UseCaseConfig::class,
    TestMemberQueryPortConfig::class,
    TestTokenValidatePortConfig::class,
)
class MemberControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var changeGlobalNicknameUseCase: ChangeGlobalNicknameUseCase

    private val authentication = UsernamePasswordAuthenticationToken("42", null, emptyList())

    @Test
    fun `닉네임 변경은 인증 회원 id와 요청 본문의 닉네임을 그대로 유스케이스에 전달한다`() {
        every { changeGlobalNicknameUseCase.change(any(), any()) } returns ChangeGlobalNicknameResult("부지런한 수달")

        mockMvc
            .patch("/api/v1/users/me/nickname") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"nickname":"부지런한 수달"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.nickname") { value("부지런한 수달") }
            }

        verify { changeGlobalNicknameUseCase.change(42L, "부지런한 수달") }
    }

    @Test
    fun `nickname이 빈 문자열이면 400과 INVALID_REQUEST로 응답한다`() {
        mockMvc
            .patch("/api/v1/users/me/nickname") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"nickname":""}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("INVALID_REQUEST") }
            }
    }

    @Test
    fun `nickname 필드 자체가 없으면 400과 INVALID_REQUEST로 응답한다`() {
        mockMvc
            .patch("/api/v1/users/me/nickname") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("INVALID_REQUEST") }
            }
    }

    @Test
    fun `닉네임 형식이 잘못되면 400과 INVALID_NICKNAME으로 응답한다`() {
        every {
            changeGlobalNicknameUseCase.change(any(), any())
        } throws BusinessException(MemberErrorCode.INVALID_NICKNAME)

        mockMvc
            .patch("/api/v1/users/me/nickname") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"nickname":"연속  공백"}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("INVALID_NICKNAME") }
            }
    }

    @TestConfiguration
    class UseCaseConfig {
        @Bean
        fun changeGlobalNicknameUseCase(): ChangeGlobalNicknameUseCase = mockk()
    }
}
