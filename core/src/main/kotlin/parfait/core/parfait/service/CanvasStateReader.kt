package parfait.core.parfait.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import parfait.core.exception.BusinessException
import parfait.core.parfait.exception.ParfaitErrorCode
import parfait.core.parfait.port.`in`.EnsureActiveCanvasUseCase
import parfait.core.parfait.port.out.CanvasCachePort
import parfait.core.parfait.port.out.ParfaitVersionPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupVersionPort
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException
import java.time.LocalDate

/** v2 today 가 304 를 판단할 때 쓰는 값들을 캐시 우선·DB 대체로 읽는다. 캐시가 비거나 죽으면 항상 DB 로 내려간다. */
@Component
class CanvasStateReader(
    private val cache: CanvasCachePort,
    private val parfaitVersionPort: ParfaitVersionPort,
    private val groupVersionPort: ParfaitGroupVersionPort,
    private val memberQueryPort: ParfaitGroupMemberQueryPort,
    private val ensureActiveCanvasUseCase: EnsureActiveCanvasUseCase,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun requireMember(
        groupId: Long,
        memberId: Long,
    ) {
        if (cache.isMember(groupId, memberId)) return
        if (!memberQueryPort.existsByGroupIdAndMemberId(groupId, memberId)) {
            throw ParfaitGroupException(ParfaitGroupError.GROUP_NOT_JOINED)
        }
        cache.putMember(groupId, memberId)
    }

    /**
     * 오늘 캔버스 id 와 그 version 을 읽는다.
     * 캐시에서 읽은 id 가 DB 에 없으면(DB 복원·초기화 후 남은 Redis 값) 그 캐시 키를 지우고 캔버스를 다시 확보해 **한 번만** 재조회한다.
     * `ensure` 로 직접 얻은 id 가 없는 경우는 진짜 오류이므로 복구하지 않고 그대로 던진다.
     */
    fun todayCanvas(
        groupId: Long,
        date: LocalDate,
    ): TodayCanvas {
        val cachedId = cache.getTodayParfaitId(groupId, date) ?: return canvasOf(ensureTodayParfaitId(groupId, date))
        return try {
            canvasOf(cachedId)
        } catch (e: BusinessException) {
            if (e.errorCode != ParfaitErrorCode.PARFAIT_NOT_FOUND) throw e
            log.warn("오늘 캔버스 id 캐시가 DB 에 없는 값을 가리켜 다시 확보합니다 groupId={} date={} parfaitId={}", groupId, date, cachedId)
            cache.evictTodayParfaitId(groupId, date)
            canvasOf(ensureTodayParfaitId(groupId, date))
        }
    }

    fun groupVersion(groupId: Long): Long =
        cache.getGroupVersion(groupId)
            ?: groupVersionPort.getVersion(groupId).also { cache.putGroupVersion(groupId, it) }

    private fun ensureTodayParfaitId(
        groupId: Long,
        date: LocalDate,
    ): Long =
        ensureActiveCanvasUseCase.ensure(groupId, date).requireId().also {
            cache.putTodayParfaitId(groupId, date, it)
        }

    private fun canvasOf(parfaitId: Long): TodayCanvas = TodayCanvas(parfaitId, parfaitVersion(parfaitId))

    private fun parfaitVersion(parfaitId: Long): Long =
        cache.getParfaitVersion(parfaitId)
            ?: parfaitVersionPort.getVersion(parfaitId).also { cache.putParfaitVersion(parfaitId, it) }
}

data class TodayCanvas(
    val parfaitId: Long,
    val version: Long,
)
