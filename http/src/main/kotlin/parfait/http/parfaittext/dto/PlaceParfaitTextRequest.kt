package parfait.http.parfaittext.dto

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import parfait.core.parfaittext.domain.TextAlign
import parfait.core.parfaittext.port.`in`.PlaceParfaitTextCommand

data class PlaceParfaitTextRequest(
    @field:Schema(description = "텍스트 내용 (공백만으로 구성할 수 없으며 최대 2000자)", example = "오늘도 행복한 하루!")
    val text: String,
    val positionX: Double,
    val positionY: Double,
    val positionZ: Int,
    @field:Schema(description = "기준 폰트 크기에 대한 확대 배율 (양수)", example = "1.5")
    val scale: Double,
    val rotation: Double,
    @field:Schema(description = "텍스트 색상 (최대 20자)", example = "#FF5733")
    val color: String,
    @field:Schema(description = "확대 전 기준 폰트 크기 (양수). 표시 크기 = fontSize × scale", example = "16.0")
    val fontSize: Double,
    @param:JsonProperty("isFill")
    @get:JsonProperty("isFill")
    @field:Schema(description = "텍스트 내부 채움 여부 (생략 시 false)")
    val isFill: Boolean = false,
    @param:JsonProperty("isStroke")
    @get:JsonProperty("isStroke")
    @field:Schema(description = "텍스트 외곽선 표시 여부 (생략 시 false)")
    val isStroke: Boolean = false,
    @field:Schema(description = "텍스트 정렬 (LEFT, CENTER, RIGHT. 생략 시 CENTER)", example = "CENTER")
    val textAlign: TextAlign = TextAlign.CENTER,
) {
    fun toCommand(
        memberId: Long,
        groupId: Long,
        parfaitId: Long,
    ): PlaceParfaitTextCommand =
        PlaceParfaitTextCommand(
            memberId = memberId,
            groupId = groupId,
            parfaitId = parfaitId,
            text = text,
            positionX = positionX,
            positionY = positionY,
            positionZ = positionZ,
            scale = scale,
            rotation = rotation,
            color = color,
            fontSize = fontSize,
            isFill = isFill,
            isStroke = isStroke,
            textAlign = textAlign,
        )
}
