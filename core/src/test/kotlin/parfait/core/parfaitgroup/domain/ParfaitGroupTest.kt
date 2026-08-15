package parfait.core.parfaitgroup.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import kotlin.test.assertFailsWith

class ParfaitGroupTest {
    private val group =
        ParfaitGroup.reconstitute(
            id = 1L,
            name = "파르페",
            inviteCode = "ABCD12",
            memberLimit = 2,
            createdAt = LocalDateTime.MIN,
            updatedAt = LocalDateTime.MIN,
        )

    @Test
    fun `이미 참여한 회원은 다시 참여할 수 없다`() {
        val exception =
            assertFailsWith<ParfaitGroupException> {
                group.validateJoin(alreadyJoined = true, currentMemberCount = 1)
            }

        exception.error shouldBe ParfaitGroupError.GROUP_ALREADY_JOINED
    }

    @Test
    fun `현재 인원이 최대 인원에 도달하면 참여할 수 없다`() {
        val exception =
            assertFailsWith<ParfaitGroupException> {
                group.validateJoin(alreadyJoined = false, currentMemberCount = 2)
            }

        exception.error shouldBe ParfaitGroupError.GROUP_MEMBER_LIMIT_REACHED
    }

    @Test
    fun `참여 이력이 없고 자리가 남아 있으면 참여할 수 있다`() {
        group.validateJoin(alreadyJoined = false, currentMemberCount = 1)
    }
}
