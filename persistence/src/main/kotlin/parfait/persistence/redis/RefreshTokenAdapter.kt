package parfait.persistence.redis

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import parfait.core.port.out.TokenSavePort
import java.time.Duration

@Component
class RefreshTokenAdapter(
    private val redisTemplate: StringRedisTemplate,
) : TokenSavePort {
    override fun save(
        memberId: Long,
        sessionId: String,
        refreshToken: String,
        ttlSeconds: Long,
    ) {
        redisTemplate.opsForValue().set(
            "refresh:$memberId:$sessionId",
            refreshToken,
            Duration.ofSeconds(ttlSeconds),
        )
    }
}
