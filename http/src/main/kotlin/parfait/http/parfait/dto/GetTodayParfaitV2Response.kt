package parfait.http.parfait.dto

import com.fasterxml.jackson.annotation.JsonProperty
import parfait.core.parfait.domain.ParfaitStatus
import parfait.core.parfait.port.`in`.GetTodayParfaitResult
import parfait.core.parfait.port.`in`.TodayParfaitImageResult
import parfait.core.parfait.port.`in`.TodayParfaitTextResult
import parfait.core.parfaitimage.domain.BorderType
import parfait.core.parfaittext.domain.TextAlign
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * v2 today 응답. 구조는 v1(`GetTodayParfaitResponse`)과 같고, 토핑 원소마다 `toppingType`을 붙이고
 * 텍스트에도 `imageUrl`(항상 null)을 내려 클라이언트가 이미지·텍스트를 같은 방식으로 다룰 수 있게 한다.
 */
data class GetTodayParfaitV2Response(
    val parfaitId: Long,
    val groupName: String,
    val date: LocalDate,
    val status: ParfaitStatus,
    val lastClosedDate: LocalDate?,
    val groupMembers: List<GroupMemberResponse>,
    val background: BackgroundResponse?,
    val images: List<TodayParfaitImageV2Response>?,
    val texts: List<TodayParfaitTextV2Response>?,
) {
    companion object {
        fun from(result: GetTodayParfaitResult): GetTodayParfaitV2Response =
            GetTodayParfaitV2Response(
                parfaitId = result.parfaitId,
                groupName = result.groupName,
                date = result.date,
                status = result.status,
                lastClosedDate = result.lastClosedDate,
                groupMembers = result.groupMembers.map(GroupMemberResponse::from),
                background = result.background?.let(BackgroundResponse::from),
                images = result.images?.map(TodayParfaitImageV2Response::from),
                texts = result.texts?.map(TodayParfaitTextV2Response::from),
            )
    }
}

enum class ToppingType {
    IMAGE,
    TEXT,
}

data class TodayParfaitImageV2Response(
    val toppingType: ToppingType,
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
        fun from(result: TodayParfaitImageResult): TodayParfaitImageV2Response =
            TodayParfaitImageV2Response(
                toppingType = ToppingType.IMAGE,
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

data class TodayParfaitTextV2Response(
    val toppingType: ToppingType,
    val parfaitTextId: Long,
    val imageUrl: String?,
    val text: String,
    val positionX: Double,
    val positionY: Double,
    val positionZ: Int,
    val scale: Double,
    val rotation: Double,
    val color: String,
    val fontSize: Double,
    @get:JsonProperty("isFill")
    val isFill: Boolean,
    @get:JsonProperty("isStroke")
    val isStroke: Boolean,
    val textAlign: TextAlign,
    val placedBy: PlacedByResponse,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(result: TodayParfaitTextResult): TodayParfaitTextV2Response =
            TodayParfaitTextV2Response(
                toppingType = ToppingType.TEXT,
                parfaitTextId = result.parfaitTextId,
                imageUrl = null,
                text = result.text,
                positionX = result.positionX,
                positionY = result.positionY,
                positionZ = result.positionZ,
                scale = result.scale,
                rotation = result.rotation,
                color = result.color,
                fontSize = result.fontSize,
                isFill = result.isFill,
                isStroke = result.isStroke,
                textAlign = result.textAlign,
                placedBy = PlacedByResponse.from(result.placedBy),
                createdAt = result.createdAt,
            )
    }
}
