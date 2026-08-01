package parfait.http.security

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.FilterChain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.servlet.HandlerExceptionResolver
import parfait.core.auth.exception.AuthErrorCode
import parfait.core.auth.port.out.TokenValidatePort
import parfait.core.exception.BusinessException
import parfait.core.member.port.out.MemberQueryPort
import kotlin.test.assertEquals

class JwtAuthFilterTest {
    private val tokenValidatePort = mockk<TokenValidatePort>()
    private val memberQueryPort = mockk<MemberQueryPort>()
    private val resolver = mockk<HandlerExceptionResolver>(relaxed = true)
    private val filter = JwtAuthFilter(tokenValidatePort, memberQueryPort, resolver)

    @AfterEach
    fun clearSecurityContext() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun `Authorization 헤더가 없으면 인증 설정 없이 체인을 그대로 통과시킨다`() {
        val request = MockHttpServletRequest("GET", "/protected")
        val response = MockHttpServletResponse()
        var chainCalled = false
        val chain = FilterChain { _, _ -> chainCalled = true }

        filter.doFilter(request, response, chain)

        assertEquals(true, chainCalled)
        assertEquals(null, SecurityContextHolder.getContext().authentication)
        verify(exactly = 0) { tokenValidatePort.validateAccessToken(any()) }
    }

    @Test
    fun `토큰 검증에 실패하면 해당 예외를 그대로 위임한다`() {
        val request = MockHttpServletRequest("GET", "/protected")
        request.addHeader("Authorization", "Bearer invalid-token")
        val response = MockHttpServletResponse()
        val chain = FilterChain { _, _ -> error("체인이 호출되면 안 된다") }
        every {
            tokenValidatePort.validateAccessToken("invalid-token")
        } throws BusinessException(AuthErrorCode.INVALID_TOKEN)

        filter.doFilter(request, response, chain)

        verify {
            resolver.resolveException(
                request,
                response,
                null,
                match { it is BusinessException && it.errorCode == AuthErrorCode.INVALID_TOKEN },
            )
        }
    }

    @Test
    fun `회원이 존재하지 않으면 MEMBER_NOT_FOUND로 예외를 위임한다`() {
        val request = MockHttpServletRequest("GET", "/protected")
        request.addHeader("Authorization", "Bearer valid-token")
        val response = MockHttpServletResponse()
        val chain = FilterChain { _, _ -> error("체인이 호출되면 안 된다") }
        every { tokenValidatePort.validateAccessToken("valid-token") } returns 42L
        every { memberQueryPort.existsById(42L) } returns false

        filter.doFilter(request, response, chain)

        verify {
            resolver.resolveException(
                request,
                response,
                null,
                match { it is BusinessException && it.errorCode == AuthErrorCode.MEMBER_NOT_FOUND },
            )
        }
    }

    @Test
    fun `토큰과 회원이 유효하면 SecurityContext에 memberId를 저장하고 체인을 진행한다`() {
        val request = MockHttpServletRequest("GET", "/protected")
        request.addHeader("Authorization", "Bearer valid-token")
        val response = MockHttpServletResponse()
        var chainCalled = false
        val chain = FilterChain { _, _ -> chainCalled = true }
        every { tokenValidatePort.validateAccessToken("valid-token") } returns 42L
        every { memberQueryPort.existsById(42L) } returns true

        filter.doFilter(request, response, chain)

        assertEquals(true, chainCalled)
        assertEquals("42", SecurityContextHolder.getContext().authentication!!.name)
    }
}
