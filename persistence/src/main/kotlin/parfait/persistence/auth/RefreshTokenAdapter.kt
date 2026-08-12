package parfait.persistence.auth

import org.springframework.data.redis.core.ScanOptions
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import parfait.core.auth.port.out.TokenDeletePort
import parfait.core.auth.port.out.TokenQueryPort
import parfait.core.auth.port.out.TokenSavePort
import java.time.Duration

@Component
class RefreshTokenAdapter(
    private val redisTemplate: StringRedisTemplate,
) : TokenSavePort,
    TokenQueryPort,
    TokenDeletePort {
    override fun save(
        memberId: Long,
        sessionId: String,
        refreshToken: String,
        ttlSeconds: Long,
    ) {
        redisTemplate.opsForValue().set(
            key(memberId, sessionId),
            refreshToken,
            Duration.ofSeconds(ttlSeconds),
        )
    }

    override fun findRefreshToken(
        memberId: Long,
        sessionId: String,
    ): String? = redisTemplate.opsForValue().get(key(memberId, sessionId))

    override fun delete(
        memberId: Long,
        sessionId: String,
    ) {
        redisTemplate.delete(key(memberId, sessionId))
    }

    override fun deleteAllByMemberId(memberId: Long) {
        val options =
            ScanOptions
                .scanOptions()
                .match("refresh:$memberId:*")
                .count(100)
                .build()
        val keysToDelete = mutableListOf<String>()
        redisTemplate.execute { connection ->
            connection.keyCommands().scan(options).use { cursor ->
                cursor.forEachRemaining { keyBytes -> keysToDelete.add(String(keyBytes, Charsets.UTF_8)) }
            }
            null
        }
        if (keysToDelete.isNotEmpty()) {
            redisTemplate.delete(keysToDelete)
        }
    }

    private fun key(
        memberId: Long,
        sessionId: String,
    ): String = "refresh:$memberId:$sessionId"
}
