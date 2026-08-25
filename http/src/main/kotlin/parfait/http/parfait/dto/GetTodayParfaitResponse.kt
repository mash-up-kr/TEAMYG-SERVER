package parfait.http.parfait.dto

import parfait.core.parfait.domain.BackgroundType
import parfait.core.parfait.domain.OwnerType
import parfait.core.parfait.domain.ParfaitStatus
import parfait.core.parfait.port.`in`.BackgroundResult
import parfait.core.parfait.port.`in`.GetTodayParfaitResult
import parfait.core.parfait.port.`in`.GroupMemberResult
import parfait.core.parfait.port.`in`.PlacedByResult
import parfait.core.parfait.port.`in`.TodayParfaitImageResult
import parfait.core.parfaitgroup.domain.NameTagChipType
import parfait.core.parfaitimage.domain.BorderType
import java.time.LocalDate
import java.time.LocalDateTime

data class GetTodayParfaitResponse(
    val parfaitId: Long,
    val date: LocalDate,
    val status: ParfaitStatus,
    val lastClosedDate: LocalDate?,
    val groupMembers: List<GroupMemberResponse>,
    val background: BackgroundResponse?,
    val images: List<TodayParfaitImageResponse>?,
) {
    companion object {
        fun from(result: GetTodayParfaitResult): GetTodayParfaitResponse =
            GetTodayParfaitResponse(
                parfaitId = result.parfaitId,
                date = result.date,
                status = result.status,
                lastClosedDate = result.lastClosedDate,
                groupMembers = result.groupMembers.map(GroupMemberResponse::from),
                background = result.background?.let(BackgroundResponse::from),
                images = result.images?.map(TodayParfaitImageResponse::from),
            )
    }
}

data class GroupMemberResponse(
    val id: Long,
    val nickname: String,
    val nameTagChip: NameTagChipType,
) {
    companion object {
        fun from(result: GroupMemberResult): GroupMemberResponse =
            GroupMemberResponse(id = result.id, nickname = result.nickname, nameTagChip = result.nametagChip)
    }
}

data class BackgroundResponse(
    val type: BackgroundType,
    val value: String,
) {
    companion object {
        fun from(result: BackgroundResult): BackgroundResponse =
            BackgroundResponse(type = result.type, value = result.value)
    }
}

data class TodayParfaitImageResponse(
    val parfaitImageId: Long,
    val imageId: Long,
    val imageUrl: String,
    val positionX: Double,
    val positionY: Double,
    val positionZ: Int,
    val scale: Double,
    val rotation: Double,
    val borderType: BorderType,
    val borderColor: String?,
    val borderWidth: Double?,
    val placedBy: PlacedByResponse,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(result: TodayParfaitImageResult): TodayParfaitImageResponse =
            TodayParfaitImageResponse(
                parfaitImageId = result.parfaitImageId,
                imageId = result.imageId,
                imageUrl = result.imageUrl,
                positionX = result.positionX,
                positionY = result.positionY,
                positionZ = result.positionZ,
                scale = result.scale,
                rotation = result.rotation,
                borderType = result.borderType,
                borderColor = result.borderColor,
                borderWidth = result.borderWidth,
                placedBy = PlacedByResponse.from(result.placedBy),
                createdAt = result.createdAt,
            )
    }
}

data class PlacedByResponse(
    val groupMemberId: Long,
    val nickname: String,
    val nameTagChip: NameTagChipType,
    val ownerType: OwnerType,
) {
    companion object {
        fun from(result: PlacedByResult): PlacedByResponse =
            PlacedByResponse(
                groupMemberId = result.groupMemberId,
                nickname = result.nickname,
                nameTagChip = result.nametagChip,
                ownerType = result.ownerType,
            )
    }
}
