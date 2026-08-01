@file:Suppress("ktlint:standard:package-name")

package parfait.core.parfaitgroup.application.port.`in`

interface PreviewParfaitGroupJoinUseCase {
    fun preview(
        memberId: Long,
        inviteCode: String,
    ): PreviewParfaitGroupJoinResult
}

data class PreviewParfaitGroupJoinResult(
    val groupName: String,
)
