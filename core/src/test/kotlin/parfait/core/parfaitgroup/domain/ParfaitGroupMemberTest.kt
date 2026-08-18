package parfait.core.parfaitgroup.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class ParfaitGroupMemberTest {
    @Test
    fun `참여 시 배정받은 Nametag-Chip 타입을 갖는다`() {
        val member =
            ParfaitGroupMember.join(
                parfaitGroupId = 1L,
                memberId = 10L,
                groupNickname = "내 닉네임",
                nametagChip = NameTagChipType.TYPE3,
            )

        member.nametagChip shouldBe NameTagChipType.TYPE3
    }

    @Test
    fun `탈퇴하면 Nametag-Chip이 DEFAULT로 반납된다`() {
        val member =
            ParfaitGroupMember.join(
                parfaitGroupId = 1L,
                memberId = 10L,
                groupNickname = "내 닉네임",
                nametagChip = NameTagChipType.TYPE3,
            )

        val left = member.leave(LocalDateTime.of(2026, 8, 17, 0, 0))

        left.nametagChip shouldBe NameTagChipType.DEFAULT
        left.leftAt shouldBe LocalDateTime.of(2026, 8, 17, 0, 0)
    }
}
