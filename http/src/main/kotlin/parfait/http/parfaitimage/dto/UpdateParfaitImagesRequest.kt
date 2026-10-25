package parfait.http.parfaitimage.dto

import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageItemCommand
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImagesCommand
import parfait.core.parfaitimage.port.`in`.UpdateParfaitTextItemCommand

data class UpdateParfaitImagesRequest(
    val items: List<UpdateParfaitImageItemRequest> = emptyList(),
    val textItems: List<UpdateParfaitTextItemRequest> = emptyList(),
) {
    fun toCommand(
        memberId: Long,
        groupId: Long,
        parfaitId: Long,
    ): UpdateParfaitImagesCommand =
        UpdateParfaitImagesCommand(
            memberId = memberId,
            groupId = groupId,
            parfaitId = parfaitId,
            items = items.map { it.toCommand() },
            textItems = textItems.map { it.toCommand() },
        )
}

data class UpdateParfaitImageItemRequest(
    val parfaitImageId: Long,
    val positionX: Double?,
    val positionY: Double?,
    val positionZ: Int?,
    val scale: Double?,
    val rotation: Double?,
) {
    fun toCommand(): UpdateParfaitImageItemCommand =
        UpdateParfaitImageItemCommand(
            parfaitImageId = parfaitImageId,
            positionX = positionX,
            positionY = positionY,
            positionZ = positionZ,
            scale = scale,
            rotation = rotation,
        )
}

data class UpdateParfaitTextItemRequest(
    val parfaitTextId: Long,
    val positionX: Double?,
    val positionY: Double?,
    val positionZ: Int?,
    val scale: Double?,
    val rotation: Double?,
) {
    fun toCommand(): UpdateParfaitTextItemCommand =
        UpdateParfaitTextItemCommand(
            parfaitTextId = parfaitTextId,
            positionX = positionX,
            positionY = positionY,
            positionZ = positionZ,
            scale = scale,
            rotation = rotation,
        )
}
