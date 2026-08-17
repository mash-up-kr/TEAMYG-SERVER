package parfait.core.parfaitgroup.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class NameTagChipTypeTest {
    @Test
    fun `점유된 타입을 제외한 나머지 중에서 배정한다`() {
        val occupied = setOf(NameTagChipType.TYPE1, NameTagChipType.TYPE2)

        val assigned = NameTagChipType.assignRandom(occupied)

        (assigned in occupied) shouldBe false
    }

    @Test
    fun `점유 타입이 없으면 12종 중 하나를 배정한다`() {
        val assigned = NameTagChipType.assignRandom(emptySet())

        (assigned in NameTagChipType.entries) shouldBe true
    }

    @Test
    fun `12종이 모두 점유되면 배정할 수 없다`() {
        assertFailsWith<IllegalStateException> {
            NameTagChipType.assignRandom((NameTagChipType.entries - NameTagChipType.RELEASED).toSet())
        }
    }

    @Test
    fun `타입 개수는 그룹 정원 최대치와 같은 12종이다`() {
        (NameTagChipType.entries - NameTagChipType.RELEASED).size shouldBe GroupMemberLimit.MAX
    }
}
