package parfait.external.discord.command

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class AdminRoleChecker(
    @Value("\${discord.bot.admin-role-id:}") adminRoleId: String,
) {
    private val adminRoleId = adminRoleId.trim()

    /** 설정이 비어 있으면 누구도 운영진이 아니다(fail closed). */
    fun isAdmin(roleIds: Collection<String>): Boolean = adminRoleId.isNotBlank() && adminRoleId in roleIds
}
