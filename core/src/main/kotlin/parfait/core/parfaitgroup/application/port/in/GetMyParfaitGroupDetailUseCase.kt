@file:Suppress("ktlint:standard:package-name")

package parfait.core.parfaitgroup.application.port.`in`

interface GetMyParfaitGroupDetailUseCase {
    fun get(
        memberId: Long,
        groupId: Long,
    ): MyParfaitGroupDetailResult
}

data class MyParfaitGroupDetailResult(
    val groupId: Long,
    val groupNickname: String,
    val inviteCode: String,
    val members: List<ParfaitGroupMemberResult>,
)

data class ParfaitGroupMemberResult(
    val memberId: Long,
    val groupNickname: String,
)
