package parfait.http.parfait.dto

import parfait.core.parfait.domain.BackgroundType
import parfait.core.parfait.port.`in`.ChangeParfaitBackgroundCommand

data class ChangeParfaitBackgroundRequest(
    val type: BackgroundType,
    val value: String?,
    val imageId: Long?,
) {
    fun toCommand(
        memberId: Long,
        groupId: Long,
        parfaitId: Long,
    ): ChangeParfaitBackgroundCommand =
        ChangeParfaitBackgroundCommand(
            memberId = memberId,
            groupId = groupId,
            parfaitId = parfaitId,
            type = type,
            value = value,
            imageId = imageId,
        )
}
