package parfait.http.parfaitgroup.dto

import parfait.core.parfaitgroup.application.port.`in`.ChangeMyParfaitGroupNicknameCommand
import parfait.core.parfaitgroup.application.port.`in`.CreateParfaitGroupCommand
import parfait.core.parfaitgroup.application.port.`in`.JoinParfaitGroupCommand
import parfait.core.parfaitgroup.application.port.`in`.ReportParfaitGroupCommand

data class JoinParfaitGroupRequest(
    val inviteCode: String,
) {
    fun toCommand(memberId: Long): JoinParfaitGroupCommand =
        JoinParfaitGroupCommand(
            memberId = memberId,
            inviteCode = inviteCode,
        )
}

data class CreateParfaitGroupRequest(
    val groupName: String,
    val groupNickname: String,
    val memberLimit: Int,
) {
    fun toCommand(memberId: Long): CreateParfaitGroupCommand =
        CreateParfaitGroupCommand(
            memberId = memberId,
            groupName = groupName,
            groupNickname = groupNickname,
            memberLimit = memberLimit,
        )
}

data class ChangeMyParfaitGroupNicknameRequest(
    val groupNickname: String,
) {
    fun toCommand(
        memberId: Long,
        groupId: Long,
    ): ChangeMyParfaitGroupNicknameCommand =
        ChangeMyParfaitGroupNicknameCommand(
            memberId = memberId,
            groupId = groupId,
            groupNickname = groupNickname,
        )
}

data class ReportParfaitGroupRequest(
    val reason: String,
) {
    fun toCommand(
        memberId: Long,
        groupId: Long,
    ): ReportParfaitGroupCommand =
        ReportParfaitGroupCommand(
            memberId = memberId,
            groupId = groupId,
            reason = reason,
        )
}
