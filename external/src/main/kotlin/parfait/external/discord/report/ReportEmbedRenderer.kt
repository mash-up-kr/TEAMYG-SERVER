package parfait.external.discord.report

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import org.springframework.stereotype.Component
import parfait.core.report.domain.ReportContent
import parfait.core.report.domain.ReportPeriod
import parfait.core.stats.domain.DailySignupStats
import parfait.core.stats.domain.SignupGrowth
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Component
class ReportEmbedRenderer {
    fun render(content: ReportContent): MessageEmbed {
        val s = content.signupStats
        val from = content.period.from.format(PERIOD_FORMAT)
        val to = content.period.to.format(PERIOD_FORMAT)
        val groups = content.newGroups?.let { "\n신규 그룹 ${it}개" } ?: ""
        val service =
            "신규 가입 ${s.newSignups}명${newSignupsChange(
                content.growth,
            )} · 탈퇴 ${s.withdrawals}명 (신규 중 ${s.newSignupWithdrawals}명) " +
                "· 순증 ${signed(s.netGrowth)}명\n" +
                "누적 가입자 ${"%,d".format(Locale.KOREA, s.totalUsers)}명${totalUsersGrowth(content.growth)}" +
                groups

        val embed =
            EmbedBuilder()
                .setTitle("📊 가입자 리포트 ($from ~ $to 기준)")
                .addField("서비스", service, false)
        dailyFields(content).forEach { (name, value) -> embed.addField(name, value, false) }
        return embed.build()
    }

    // 임베드 필드 값은 1024자 제한이 있어 7일씩 나눠 담는다.
    private fun dailyFields(content: ReportContent): List<Pair<String, String>> =
        content.daily.chunked(DAILY_CHUNK_SIZE).map { chunk ->
            val name = "일별 ${chunk.first().date.format(DAY_FORMAT)}~${chunk.last().date.format(DAY_FORMAT)}"
            name to chunk.joinToString("\n") { dailyLine(it, content.period) }
        }

    private fun dailyLine(
        day: DailySignupStats,
        period: ReportPeriod,
    ): String {
        val weekday = day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)
        val inProgress = day.date == period.to.toLocalDate() && period.to.toLocalTime() != LocalTime.MIDNIGHT
        val suffix = if (inProgress) " (진행 중)" else ""
        return "${day.date.format(DAY_FORMAT)} ($weekday) : 가입 +${day.newSignups}명 / 탈퇴 -${day.withdrawals}명$suffix"
    }

    private fun signed(value: Long): String = if (value > 0) "+$value" else value.toString()

    private fun newSignupsChange(growth: SignupGrowth?): String {
        if (growth == null) return ""
        val percent = growth.newSignupsChangePercent ?: return " (직전 0명)"
        val rounded = Math.round(percent)
        return when {
            rounded > 0 -> " (▲$rounded%)"
            rounded < 0 -> " (▼${-rounded}%)"
            else -> " (±0%)"
        }
    }

    private fun totalUsersGrowth(growth: SignupGrowth?): String {
        val percent = growth?.totalUsersGrowthPercent ?: return ""
        val rounded = Math.round(percent * 10) / 10.0
        val text = "%.1f".format(Locale.KOREA, rounded)
        return if (rounded > 0) " (+$text%)" else " ($text%)"
    }

    companion object {
        private val PERIOD_FORMAT = DateTimeFormatter.ofPattern("M/d HH:mm")
        private val DAY_FORMAT = DateTimeFormatter.ofPattern("MM/dd")
        private const val DAILY_CHUNK_SIZE = 7
    }
}
