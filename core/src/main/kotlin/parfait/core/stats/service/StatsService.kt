package parfait.core.stats.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import parfait.core.exception.BusinessException
import parfait.core.stats.domain.SignupStats
import parfait.core.stats.domain.SignupStatsComparison
import parfait.core.stats.exception.StatsErrorCode
import parfait.core.stats.port.`in`.GetSignupStatsComparisonUseCase
import parfait.core.stats.port.`in`.GetSignupStatsUseCase
import parfait.core.stats.port.out.SignupStatsQueryPort
import java.time.Duration
import java.time.LocalDateTime

@Service
class StatsService(
    private val signupStatsQueryPort: SignupStatsQueryPort,
) : GetSignupStatsUseCase,
    GetSignupStatsComparisonUseCase {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun getSignupStats(
        from: LocalDateTime,
        to: LocalDateTime,
    ): SignupStats {
        if (!from.isBefore(to) ||
            to.isAfter(from.plusDays(MAX_RANGE_DAYS))
        ) {
            throw BusinessException(StatsErrorCode.INVALID_STATS_PERIOD)
        }
        return signupStatsQueryPort.aggregate(from, to)
    }

    override fun getComparison(
        from: LocalDateTime,
        to: LocalDateTime,
    ): SignupStatsComparison {
        val current = getSignupStats(from, to)
        val previousFrom = from.minus(Duration.between(from, to))
        val previous =
            try {
                signupStatsQueryPort.aggregate(previousFrom, from)
            } catch (e: Exception) {
                log.warn("직전 기간 통계 조회 실패 - 증감률을 생략한다. from={}, to={}: {}", previousFrom, from, e.message)
                null
            }
        return SignupStatsComparison(current, previous)
    }

    private companion object {
        const val MAX_RANGE_DAYS = 366L
    }
}
