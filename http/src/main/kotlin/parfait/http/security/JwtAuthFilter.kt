package parfait.http.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.servlet.HandlerExceptionResolver
import parfait.core.exception.AuthErrorCode
import parfait.core.exception.BusinessException
import parfait.core.port.out.MemberQueryPort
import parfait.core.port.out.TokenValidatePort

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
                val memberId = authenticate(token)
                SecurityContextHolder.getContext().authentication =
                    UsernamePasswordAuthenticationToken(memberId.toString(), null, emptyList())
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

    private fun authenticate(token: String): Long {
        val memberId = tokenValidatePort.validateAccessToken(token)
        if (!memberQueryPort.existsById(memberId)) {
            throw BusinessException(AuthErrorCode.MEMBER_NOT_FOUND)
        }
        return memberId
    }
}
