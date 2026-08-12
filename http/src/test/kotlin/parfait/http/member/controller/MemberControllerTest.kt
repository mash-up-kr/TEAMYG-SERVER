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
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import parfait.core.auth.domain.LoginProvider
import parfait.core.exception.BusinessException
import parfait.core.member.exception.MemberErrorCode
import parfait.core.member.port.`in`.ChangeGlobalNicknameResult
import parfait.core.member.port.`in`.ChangeGlobalNicknameUseCase
import parfait.core.member.port.`in`.GetMyAccountUseCase
import parfait.core.member.port.`in`.MyAccountResult
import parfait.core.member.port.`in`.WithdrawUseCase
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

    @Autowired
    private lateinit var withdrawUseCase: WithdrawUseCase

    @Autowired
    private lateinit var getMyAccountUseCase: GetMyAccountUseCase

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

    @Test
    fun `탈퇴는 인증 회원 id를 그대로 유스케이스에 전달하고 204를 반환한다`() {
        every { withdrawUseCase.withdraw(any()) } returns Unit

        mockMvc
            .delete("/api/v1/users/me") {
                principal = authentication
            }.andExpect {
                status { isNoContent() }
            }

        verify { withdrawUseCase.withdraw(42L) }
    }

    @Test
    fun `내 계정 정보를 조회하면 인증 회원 id로 조회한 결과를 그대로 응답한다`() {
        every { getMyAccountUseCase.getMyAccount(42L) } returns MyAccountResult(42L, LoginProvider.KAKAO, "행복한 판다")

        mockMvc
            .get("/api/v1/users/me") {
                principal = authentication
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.memberId") { value(42) }
                jsonPath("$.data.provider") { value("KAKAO") }
                jsonPath("$.data.nickname") { value("행복한 판다") }
            }

        verify { getMyAccountUseCase.getMyAccount(42L) }
    }

    @Test
    fun `존재하지 않는 회원이면 404와 MEMBER_NOT_FOUND로 응답한다`() {
        every { getMyAccountUseCase.getMyAccount(42L) } throws BusinessException(MemberErrorCode.MEMBER_NOT_FOUND)

        mockMvc
            .get("/api/v1/users/me") {
                principal = authentication
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("MEMBER_NOT_FOUND") }
            }
    }

    @TestConfiguration
    class UseCaseConfig {
        @Bean
        fun changeGlobalNicknameUseCase(): ChangeGlobalNicknameUseCase = mockk()

        @Bean
        fun withdrawUseCase(): WithdrawUseCase = mockk()

        @Bean
        fun getMyAccountUseCase(): GetMyAccountUseCase = mockk()
    }
}
