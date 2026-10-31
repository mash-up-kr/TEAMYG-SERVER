package parfait.external.discord.command

import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

private val KST: ZoneId = ZoneId.of("Asia/Seoul")

fun fixedClock(now: LocalDateTime): Clock = Clock.fixed(now.atZone(KST).toInstant(), KST)
