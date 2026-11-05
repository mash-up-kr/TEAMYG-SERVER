package parfait.core.stats.domain

import java.time.LocalDate

/** 하루(달력 날짜)의 가입·탈퇴 수. */
data class DailySignupStats(
    val date: LocalDate,
    val newSignups: Long,
    val withdrawals: Long,
)
