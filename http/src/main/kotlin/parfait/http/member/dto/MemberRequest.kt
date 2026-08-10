@file:Suppress("ktlint:standard:filename")

package parfait.http.member.dto

import jakarta.validation.constraints.NotBlank

data class ChangeGlobalNicknameRequest(
    @field:NotBlank val nickname: String,
)
