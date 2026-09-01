package parfait.http.global.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerExceptionResolver
import parfait.core.auth.exception.AuthErrorCode
import parfait.core.auth.port.out.AccessTokenClaims
import parfait.core.auth.port.out.TokenValidatePort
import parfait.core.exception.BusinessException
import parfait.core.member.port.out.MemberQueryPort

@Component
class JwtAuthFilter(
    private val tokenValidatePort: TokenValidatePort,
    private val memberQueryPort: MemberQueryPort,
    @Qualifier("handlerExceptionResolver") private val resolver: HandlerExceptionResolver,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        try {
            extractToken(request)?.let { token ->
                val claims = authenticate(token)
                // principal은 memberId 문자열 유지, sessionId는 credentials 슬롯에 실어 컨트롤러가 꺼내 쓴다
                SecurityContextHolder.getContext().authentication =
                    UsernamePasswordAuthenticationToken(claims.memberId.toString(), claims.sessionId, emptyList())
            }
            filterChain.doFilter(request, response)
        } catch (e: BusinessException) {
            resolver.resolveException(request, response, null, e)
        }
    }

    private fun extractToken(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization")
        if (header == null || !header.startsWith("Bearer ")) {
            return null
        }
        return header.removePrefix("Bearer ")
    }

    private fun authenticate(token: String): AccessTokenClaims {
        val claims = tokenValidatePort.validateAccessToken(token)
        if (!memberQueryPort.existsById(claims.memberId)) {
            throw BusinessException(AuthErrorCode.MEMBER_NOT_FOUND)
        }
        return claims
    }
}
