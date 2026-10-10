package parfait.persistence.cache

import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import parfait.core.parfait.port.out.CanvasCachePort
import java.time.Duration
import java.time.LocalDate

/**
 * Redis 는 사본이다. 모든 Redis 예외는 잡아서 읽기는 캐시 없음(null/false), 쓰기·삭제는 무시로 처리해 호출자가 DB 로 내려가게 한다.
 * `parfait.canvas-cache.enabled=false` 면 이 빈 대신 [NoOpCanvasCacheAdapter] 가 쓰인다.
 */
@Component
@ConditionalOnProperty(prefix = "parfait.canvas-cache", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class RedisCanvasCacheAdapter(
    private val redisTemplate: StringRedisTemplate,
    private val meterRegistry: MeterRegistry?,
) : CanvasCachePort {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun getParfaitVersion(parfaitId: Long): Long? =
        read("parfait_version", "parfait:version:$parfaitId") { it.toLongOrNull() }

    override fun putParfaitVersion(
        parfaitId: Long,
        version: Long,
    ) = write("parfait:version:$parfaitId", version.toString(), VERSION_TTL)

    override fun evictParfaitVersion(parfaitId: Long) = delete("parfait:version:$parfaitId")

    override fun getGroupVersion(groupId: Long): Long? =
        read("group_version", "group:version:$groupId") {
            it.toLongOrNull()
        }

    override fun putGroupVersion(
        groupId: Long,
        version: Long,
    ) = write("group:version:$groupId", version.toString(), VERSION_TTL)

    override fun evictGroupVersion(groupId: Long) = delete("group:version:$groupId")

    override fun getTodayParfaitId(
        groupId: Long,
        date: LocalDate,
    ): Long? = read("today", todayKey(groupId, date)) { it.toLongOrNull() }

    override fun putTodayParfaitId(
        groupId: Long,
        date: LocalDate,
        parfaitId: Long,
    ) = write(todayKey(groupId, date), parfaitId.toString(), TODAY_TTL)

    override fun evictTodayParfaitId(
        groupId: Long,
        date: LocalDate,
    ) = delete(todayKey(groupId, date))

    override fun isMember(
        groupId: Long,
        memberId: Long,
    ): Boolean = read("member", memberKey(groupId, memberId)) { it } != null

    override fun putMember(
        groupId: Long,
        memberId: Long,
    ) = write(memberKey(groupId, memberId), "1", MEMBER_TTL)

    override fun evictMember(
        groupId: Long,
        memberId: Long,
    ) = delete(memberKey(groupId, memberId))

    private fun todayKey(
        groupId: Long,
        date: LocalDate,
    ) = "parfait:today:$groupId:$date"

    private fun memberKey(
        groupId: Long,
        memberId: Long,
    ) = "group:member:$groupId:$memberId"

    /**
     * 값을 읽어 [parse] 로 변환한다. 결과는 hit(변환 성공)·miss(키 없음 또는 변환 실패)·error(Redis 예외)로 센다.
     * Redis 장애를 miss 로 세면 장애가 단순 캐시 미스율 상승으로 묻히므로 error 로 따로 센다.
     */
    private fun <T : Any> read(
        keyTag: String,
        key: String,
        parse: (String) -> T?,
    ): T? {
        val raw =
            try {
                redisTemplate.opsForValue().get(key)
            } catch (e: RuntimeException) {
                log.warn("canvas cache 조회 실패, DB 로 대체합니다 key={}", key, e)
                count("error", keyTag)
                return null
            }
        val value = raw?.let(parse)
        count(if (value != null) "hit" else "miss", keyTag)
        return value
    }

    private fun count(
        result: String,
        keyTag: String,
    ) {
        meterRegistry?.counter(METRIC, "result", result, "key", keyTag)?.increment()
    }

    private fun write(
        key: String,
        value: String,
        ttl: Duration,
    ) {
        try {
            redisTemplate.opsForValue().set(key, value, ttl)
        } catch (e: RuntimeException) {
            log.warn("canvas cache 저장 실패 key={}", key, e)
        }
    }

    private fun delete(key: String) {
        try {
            redisTemplate.delete(key)
        } catch (e: RuntimeException) {
            log.warn("canvas cache 삭제 실패 key={}", key, e)
        }
    }

    private companion object {
        const val METRIC = "parfait.version.cache"
        val VERSION_TTL: Duration = Duration.ofMinutes(5)
        val MEMBER_TTL: Duration = Duration.ofMinutes(5)
        val TODAY_TTL: Duration = Duration.ofHours(25)
    }
}
