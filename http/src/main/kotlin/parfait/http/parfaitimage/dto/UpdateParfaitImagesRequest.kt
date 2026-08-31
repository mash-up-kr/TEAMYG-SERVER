package parfait.http.parfaitimage.dto

import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageItemCommand
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImagesCommand

data class UpdateParfaitImagesRequest(
    val items: List<UpdateParfaitImageItemRequest>,
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
