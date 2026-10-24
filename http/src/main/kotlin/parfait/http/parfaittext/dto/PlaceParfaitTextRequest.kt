package parfait.http.parfaittext.dto

import io.swagger.v3.oas.annotations.media.Schema
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
        )
}
