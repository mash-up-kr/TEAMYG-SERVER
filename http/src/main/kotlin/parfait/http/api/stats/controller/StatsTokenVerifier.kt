package parfait.http.api.stats.controller

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import parfait.core.exception.BusinessException
import parfait.core.stats.exception.StatsErrorCode
import java.security.MessageDigest

/**
 * 통계 API 전용 정적 토큰 검증. `Authorization: Bearer` 는 `JwtAuthFilter` 가 JWT 로 검증하므로
 * 충돌을 피하려고 `X-Stats-Token` 헤더를 쓴다. 설정값이 비어 있으면 모든 요청을 거절한다.
 */
@Component
class StatsTokenVerifier(
    @Value("\${stats.api-token:}") private val expectedToken: String,
) {
    fun verify(token: String?) {
        val matches =
            expectedToken.isNotBlank() &&
                token != null &&
                MessageDigest.isEqual(expectedToken.toByteArray(Charsets.UTF_8), token.toByteArray(Charsets.UTF_8))
        if (!matches) throw BusinessException(StatsErrorCode.INVALID_STATS_TOKEN)
    }
}
