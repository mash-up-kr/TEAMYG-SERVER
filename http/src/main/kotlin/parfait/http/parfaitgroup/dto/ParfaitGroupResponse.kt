package parfait.http.parfaitgroup.dto

import parfait.core.parfaitgroup.application.port.`in`.ChangeMyParfaitGroupNicknameResult
import parfait.core.parfaitgroup.application.port.`in`.CreateParfaitGroupResult
import parfait.core.parfaitgroup.application.port.`in`.JoinParfaitGroupResult
import parfait.core.parfaitgroup.application.port.`in`.LeaveParfaitGroupResult
import parfait.core.parfaitgroup.application.port.`in`.MyParfaitGroupDetailResult
import parfait.core.parfaitgroup.application.port.`in`.MyParfaitGroupResult
import parfait.core.parfaitgroup.application.port.`in`.ParfaitGroupMemberResult
import parfait.core.parfaitgroup.application.port.`in`.PreviewParfaitGroupJoinResult
import parfait.core.parfaitgroup.application.port.`in`.ReportParfaitGroupResult
import parfait.core.parfaitgroup.domain.NameTagChipType
import java.time.LocalDateTime

data class PreviewParfaitGroupJoinResponse(
    val groupName: String,
) {
    companion object {
        fun from(result: PreviewParfaitGroupJoinResult): PreviewParfaitGroupJoinResponse =
            PreviewParfaitGroupJoinResponse(groupName = result.groupName)
    }
}

data class JoinParfaitGroupResponse(
    val groupId: Long,
    val groupName: String,
) {
    companion object {
        fun from(result: JoinParfaitGroupResult): JoinParfaitGroupResponse =
            JoinParfaitGroupResponse(
                groupId = result.groupId,
                groupName = result.groupName,
            )
    }
}

data class CreateParfaitGroupResponse(
    val groupId: Long,
    val groupName: String,
    val inviteCode: String,
    val memberLimit: Int,
    val recentImageUrl: String?,
    val recentImageUploadedAt: LocalDateTime,
    val lastPlacedByNametagChip: NameTagChipType,
) {
    companion object {
        fun from(result: CreateParfaitGroupResult): CreateParfaitGroupResponse =
            CreateParfaitGroupResponse(
                groupId = result.groupId,
                groupName = result.groupName,
                inviteCode = result.inviteCode,
                memberLimit = result.memberLimit,
                recentImageUrl = result.recentImageUrl,
                recentImageUploadedAt = result.recentImageUploadedAt,
                lastPlacedByNametagChip = result.lastPlacedByNametagChip,
            )
    }
}

data class MyParfaitGroupResponse(
    val groupId: Long,
    val groupName: String,
    val recentImageUrl: String?,
    val recentImageUploadedAt: LocalDateTime,
    val lastPlacedByNametagChip: NameTagChipType,
) {
    companion object {
        fun from(result: MyParfaitGroupResult): MyParfaitGroupResponse =
            MyParfaitGroupResponse(
                groupId = result.groupId,
                groupName = result.groupName,
                recentImageUrl = result.recentImageUrl,
                recentImageUploadedAt = result.recentImageUploadedAt,
                lastPlacedByNametagChip = result.lastPlacedByNametagChip,
            )
    }
}

data class MyParfaitGroupDetailResponse(
    val groupId: Long,
    val groupName: String,
    val groupNickname: String,
    val inviteCode: String,
    val memberLimit: Int,
    val members: List<ParfaitGroupMemberResponse>,
) {
    companion object {
        fun from(result: MyParfaitGroupDetailResult): MyParfaitGroupDetailResponse =
            MyParfaitGroupDetailResponse(
                groupId = result.groupId,
                groupName = result.groupName,
                groupNickname = result.groupNickname,
                inviteCode = result.inviteCode,
                memberLimit = result.memberLimit,
                members = result.members.map(ParfaitGroupMemberResponse::from),
            )
    }
}

data class ParfaitGroupMemberResponse(
    val memberId: Long,
    val groupNickname: String,
    val nametagChip: NameTagChipType,
) {
    companion object {
        fun from(result: ParfaitGroupMemberResult): ParfaitGroupMemberResponse =
            ParfaitGroupMemberResponse(
                memberId = result.memberId,
                groupNickname = result.groupNickname,
                nametagChip = result.nametagChip,
            )
    }
}

data class ChangeMyParfaitGroupNicknameResponse(
    val groupId: Long,
    val groupNickname: String,
) {
    companion object {
        fun from(result: ChangeMyParfaitGroupNicknameResult): ChangeMyParfaitGroupNicknameResponse =
            ChangeMyParfaitGroupNicknameResponse(
                groupId = result.groupId,
                groupNickname = result.groupNickname,
            )
    }
}

data class LeaveParfaitGroupResponse(
    val groupId: Long,
) {
    companion object {
        fun from(result: LeaveParfaitGroupResult): LeaveParfaitGroupResponse =
            LeaveParfaitGroupResponse(groupId = result.groupId)
    }
}

data class ReportParfaitGroupResponse(
    val groupId: Long,
    val reportId: Long,
) {
    companion object {
        fun from(result: ReportParfaitGroupResult): ReportParfaitGroupResponse =
            ReportParfaitGroupResponse(
                groupId = result.groupId,
                reportId = result.reportId,
            )
    }
}
