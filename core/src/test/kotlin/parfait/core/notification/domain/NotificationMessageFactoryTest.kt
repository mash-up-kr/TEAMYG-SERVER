package parfait.core.notification.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate

class NotificationMessageFactoryTest {
    private val factory = NotificationMessageFactory()
    private val date = LocalDate.of(2026, 9, 2)

    @Test
    fun `토핑 알림 - 제목에 그룹명, 본문에 닉네임`() {
        val message = factory.toppingPlaced(groupName = "우리팀", actorNickname = "체리", groupId = 50L, parfaitDate = date)

        message.title shouldBe "우리팀 파르페에 체리 하나 톡!"
        message.body shouldBe "체리님이 새 토핑을 쌓았어요"
        message.data shouldBe
            mapOf(
                "type" to "TOPPING",
                "route" to "canvas",
                "groupId" to "50",
                "date" to "2026-09-02",
            )
        message.ttl shouldBe Duration.ofHours(6)
    }

    @Test
    fun `토핑 알림 - 닉네임이 정확히 8자면 자르지 않는다`() {
        val message =
            factory.toppingPlaced(
                groupName = "우리팀",
                actorNickname = "WWWWWWWW",
                groupId = 50L,
                parfaitDate = date,
            )

        message.body shouldBe "WWWWWWWW님이 새 토핑을 쌓았어요"
    }

    @Test
    fun `토핑 알림 - 닉네임이 8자 초과면 앞 8자 + 마침표 3개`() {
        val message =
            factory.toppingPlaced(
                groupName = "우리팀",
                actorNickname = "WWWWWWWWWWW",
                groupId = 50L,
                parfaitDate = date,
            )

        message.body shouldBe "WWWWWWWW...님이 새 토핑을 쌓았어요"
    }

    @Test
    fun `토핑 알림 - 작성자 닉네임이 null 이면 익명 문구 (E-05)`() {
        val message = factory.toppingPlaced(groupName = "우리팀", actorNickname = null, groupId = 50L, parfaitDate = date)

        message.body shouldBe "누군가 새 토핑을 쌓았어요"
    }

    @Test
    fun `오전 리마인드 - P-02 문구와 data`() {
        val message = factory.dailyReminder(ReminderType.MORNING)

        message.title shouldBe "새벽 3시에 오늘의 새 캔버스가 열렸어요"
        message.body shouldBe "오늘의 첫 토핑을 쌓아볼까요?"
        message.data shouldBe mapOf("type" to "REMIND_AM", "route" to "group")
        message.ttl shouldBe Duration.ofHours(1)
    }

    @Test
    fun `저녁 리마인드 - P-03 문구와 data`() {
        val message = factory.dailyReminder(ReminderType.EVENING)

        message.title shouldBe "새벽 3시에 오늘의 캔버스가 마감돼요"
        message.body shouldBe "오늘의 마지막 토핑을 올리러 가볼까요?"
        message.data shouldBe mapOf("type" to "REMIND_PM", "route" to "group")
        message.ttl shouldBe Duration.ofHours(1)
    }
}
