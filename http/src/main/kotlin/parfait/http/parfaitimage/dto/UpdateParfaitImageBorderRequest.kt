package parfait.http.parfaitimage.dto

import parfait.core.parfaitimage.domain.BorderType
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageBorderCommand

data class UpdateParfaitImageBorderRequest(
    val borderType: BorderType,
    val borderColor: String?,
    val borderWidth: Double?,
) {
    fun toCommand(
        memberId: Long,
        groupId: Long,
        parfaitId: Long,
        parfaitImageId: Long,
    ): UpdateParfaitImageBorderCommand =
        UpdateParfaitImageBorderCommand(
            memberId = memberId,
            groupId = groupId,
            parfaitId = parfaitId,
            parfaitImageId = parfaitImageId,
            borderType = borderType,
            borderColor = borderColor,
            borderWidth = borderWidth,
        )
}
