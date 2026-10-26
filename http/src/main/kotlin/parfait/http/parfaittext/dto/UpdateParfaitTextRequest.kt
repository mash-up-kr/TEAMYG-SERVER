package parfait.http.parfaittext.dto

import com.fasterxml.jackson.annotation.JsonProperty
import parfait.core.parfaittext.domain.TextAlign
import parfait.core.parfaittext.port.`in`.UpdateParfaitTextCommand

data class UpdateParfaitTextRequest(
    val positionX: Double? = null,
    val positionY: Double? = null,
    val positionZ: Int? = null,
    val scale: Double? = null,
    val rotation: Double? = null,
    val color: String? = null,
    val fontSize: Double? = null,
    @param:JsonProperty("isFill")
    @get:JsonProperty("isFill")
    val isFill: Boolean? = null,
    @param:JsonProperty("isStroke")
    @get:JsonProperty("isStroke")
    val isStroke: Boolean? = null,
    val textAlign: TextAlign? = null,
) {
    fun toCommand(
        memberId: Long,
        groupId: Long,
        parfaitId: Long,
        parfaitTextId: Long,
    ): UpdateParfaitTextCommand =
        UpdateParfaitTextCommand(
            memberId = memberId,
            groupId = groupId,
            parfaitId = parfaitId,
            parfaitTextId = parfaitTextId,
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
