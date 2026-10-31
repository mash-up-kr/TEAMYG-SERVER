package parfait.external.discord.command

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AdminRoleCheckerTest {
    @Test
    fun `운영진 역할을 가지고 있으면 true다`() {
        AdminRoleChecker("111").isAdmin(listOf("222", "111")) shouldBe true
    }

    @Test
    fun `운영진 역할이 없으면 false다`() {
        AdminRoleChecker("111").isAdmin(listOf("222")) shouldBe false
        AdminRoleChecker("111").isAdmin(emptyList()) shouldBe false
    }

    @Test
    fun `역할 ID 설정이 비어 있으면 누구도 운영진이 아니다`() {
        AdminRoleChecker("").isAdmin(listOf("111", "")) shouldBe false
    }

    @Test
    fun `설정값 앞뒤 공백은 무시하고 매칭한다`() {
        AdminRoleChecker(" 111 ").isAdmin(listOf("111")) shouldBe true
    }

    @Test
    fun `공백뿐인 설정값이면 누구도 운영진이 아니다`() {
        AdminRoleChecker("   ").isAdmin(listOf("", "   ")) shouldBe false
    }
}
