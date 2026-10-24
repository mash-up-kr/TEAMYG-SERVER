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
        message.body shouldBe "체리님이 새 사진을 쌓았어요"
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

        message.body shouldBe "WWWWWWWW님이 새 사진을 쌓았어요"
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

        message.body shouldBe "WWWWWWWW...님이 새 사진을 쌓았어요"
    }

    @Test
    fun `토핑 알림 - 작성자 닉네임이 null 이면 익명 문구 (E-05)`() {
        val message = factory.toppingPlaced(groupName = "우리팀", actorNickname = null, groupId = 50L, parfaitDate = date)

        message.body shouldBe "누군가 새 사진을 쌓았어요"
    }

    @Test
    fun `오전 리마인드 - P-02 문구와 data`() {
        val message = factory.dailyReminder(ReminderType.MORNING)

        message.title shouldBe "새벽 3시에 오늘의 새 캔버스가 열렸어요"
        message.body shouldBe "오늘의 파르페를 먼저 쌓아볼까요?"
        message.data shouldBe mapOf("type" to "REMIND_AM", "route" to "group")
        message.ttl shouldBe Duration.ofHours(1)
    }

    @Test
    fun `저녁 리마인드 - P-03 문구와 data`() {
        val message = factory.dailyReminder(ReminderType.EVENING)

        message.title shouldBe "새벽 3시에 오늘의 캔버스가 마감돼요"
        message.body shouldBe "오늘의 파르페를 완성하러 가볼까요?"
        message.data shouldBe mapOf("type" to "REMIND_PM", "route" to "group")
        message.ttl shouldBe Duration.ofHours(1)
    }

    @Test
    fun `배경 알림 - 제목에 그룹명, 본문에 닉네임`() {
        val message =
            factory.backgroundChanged(
                groupName = "우리팀",
                actorNickname = "체리",
                groupId = 50L,
                parfaitDate = date,
            )

        message.title shouldBe "우리팀 파르페에 체리 하나 톡!"
        message.body shouldBe "체리님이 배경을 바꿨어요"
        message.data shouldBe
            mapOf(
                "type" to "BACKGROUND",
                "route" to "canvas",
                "groupId" to "50",
                "date" to "2026-09-02",
            )
        message.ttl shouldBe Duration.ofHours(6)
    }

    @Test
    fun `배경 알림 - 닉네임이 8자 초과면 앞 8자 + 마침표 3개`() {
        val message =
            factory.backgroundChanged(
                groupName = "우리팀",
                actorNickname = "WWWWWWWWWWW",
                groupId = 50L,
                parfaitDate = date,
            )

        message.body shouldBe "WWWWWWWW...님이 배경을 바꿨어요"
    }

    @Test
    fun `배경 알림 - 작성자 닉네임이 null 이면 익명 문구 (E-05)`() {
        val message =
            factory.backgroundChanged(
                groupName = "우리팀",
                actorNickname = null,
                groupId = 50L,
                parfaitDate = date,
            )

        message.body shouldBe "누군가 배경을 바꿨어요"
    }

    // --- P-04 텍스트 알림 ---

    @Test
    fun `텍스트 알림 - 제목·본문·data·TTL`() {
        val msg =
            factory.textPlaced(
                groupName = "우리팀",
                actorNickname = "체리",
                groupId = 50L,
                parfaitDate = date,
                textPreview = "오늘 다들 뭐 먹었어",
            )

        msg.title shouldBe "우리팀 파르페에 체리 하나 톡!"
        msg.body shouldBe "체리: 오늘 다들 뭐 먹었어"
        msg.data shouldBe
            mapOf(
                "type" to "TEXT",
                "route" to "canvas",
                "groupId" to "50",
                "date" to "2026-09-02",
            )
        msg.ttl shouldBe Duration.ofHours(6)
    }

    @Test
    fun `텍스트 알림 - 작성자 탈퇴 시 누군가 (E-05)`() {
        val msg =
            factory.textPlaced(
                groupName = "우리팀",
                actorNickname = null,
                groupId = 50L,
                parfaitDate = date,
                textPreview = "안녕",
            )

        msg.body shouldBe "누군가: 안녕"
    }

    // --- 미리보기 생성 규칙 ---

    @Test
    fun `미리보기 - 줄바꿈을 공백으로 치환한다 (규칙 1)`() {
        factory.generateTextPreview("안녕\n파르페") shouldBe "안녕 파르페"
        factory.generateTextPreview("a\r\nb") shouldBe "a b"
        factory.generateTextPreview("a\r\n\nb") shouldBe "a b"
    }

    @Test
    fun `미리보기 - 연속 공백을 하나로 압축한다 (규칙 1)`() {
        factory.generateTextPreview("a  b") shouldBe "a b"
        factory.generateTextPreview("a   b") shouldBe "a b"
    }

    @Test
    fun `미리보기 - 앞뒤 공백을 제거한다 (규칙 2)`() {
        factory.generateTextPreview("  안녕  ") shouldBe "안녕"
    }

    @Test
    fun `미리보기 - 11자 이하면 그대로 반환한다 (규칙 3)`() {
        factory.generateTextPreview("안녕하세요가나다라마바") shouldBe "안녕하세요가나다라마바" // 11자
        factory.generateTextPreview("안녕하세요가나다라마") shouldBe "안녕하세요가나다라마" // 10자
    }

    @Test
    fun `미리보기 - 12자 이상이면 앞 10자 + 말줄임표로 자른다 (규칙 3)`() {
        factory.generateTextPreview("안녕하세요가나다라마바사") shouldBe "안녕하세요가나다라마…" // 12자 → 10 + …
    }

    @Test
    fun `미리보기 - 이모지는 String length 가 아닌 grapheme cluster 기준으로 센다 (규칙 4)`() {
        // 😀 는 String.length=2 지만 grapheme=1
        val twelveEmojis = "😀😁😂😃😄😅😆😇😈😉😊😋" // grapheme 12개
        factory.generateTextPreview(twelveEmojis) shouldBe "😀😁😂😃😄😅😆😇😈😉…"

        val elevenEmojis = "😀😁😂😃😄😅😆😇😈😉😊" // grapheme 11개 → 그대로
        factory.generateTextPreview(elevenEmojis) shouldBe elevenEmojis
    }

    @Test
    fun `미리보기 - 이모지 중간에서 자르지 않는다 (규칙 4)`() {
        // 10번째 자리가 이모지인 경우: 이모지를 완전히 포함해 앞 10개를 유지해야 한다
        val text = "가나다라마바사아자😀차카" // grapheme 12개
        factory.generateTextPreview(text) shouldBe "가나다라마바사아자😀…"
    }

    @Test
    fun `미리보기 - 정규화 후 빈 문자열이면 null 반환 (E-22)`() {
        factory.generateTextPreview("") shouldBe null
        factory.generateTextPreview("   ") shouldBe null
        factory.generateTextPreview("\n\n") shouldBe null
        factory.generateTextPreview(" \r\n ") shouldBe null
    }
}
