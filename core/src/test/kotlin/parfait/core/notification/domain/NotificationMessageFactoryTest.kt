package parfait.core.notification.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate

class NotificationMessageFactoryTest {
    private val factory = NotificationMessageFactory()
    private val date = LocalDate.of(2026, 9, 2)

    @Test
    fun `작성자 닉네임이 있으면 제목에 그룹명, 본문에 닉네임을 넣는다`() {
        val message = factory.toppingPlaced(groupName = "우리팀", actorNickname = "체리", groupId = 50L, parfaitDate = date)

        message.title shouldBe "우리팀 파르페에 체리 얹을 타이밍!"
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
    fun `작성자 닉네임이 null 이면 본문을 익명 문구로 치환한다 (E-05)`() {
        val message = factory.toppingPlaced(groupName = "우리팀", actorNickname = null, groupId = 50L, parfaitDate = date)

        message.body shouldBe "누군가 새 토핑을 쌓았어요"
    }
}
