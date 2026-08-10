@file:Suppress("ktlint:standard:filename")

package parfait.http.member.dto

data class ChangeGlobalNicknameResponse(
    val nickname: String,
)

data class MyAccountResponse(
    val memberId: Long,
    val provider: String,
    val nickname: String,
)
