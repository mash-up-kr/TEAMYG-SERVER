package parfait.core.parfait.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import parfait.core.exception.BusinessException
import parfait.core.parfait.domain.OwnerType
import parfait.core.parfait.exception.ParfaitErrorCode
import parfait.core.parfait.port.`in`.BackgroundResult
import parfait.core.parfait.port.`in`.GetParfaitDetailCommand
import parfait.core.parfait.port.`in`.GetParfaitDetailUseCase
import parfait.core.parfait.port.`in`.GetTodayParfaitResult
import parfait.core.parfait.port.`in`.GroupMemberResult
import parfait.core.parfait.port.`in`.PlacedByResult
import parfait.core.parfait.port.`in`.TodayParfaitImageResult
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException
import parfait.core.parfaitimage.port.out.ParfaitImageQueryPort

@Service
class GetParfaitDetailService(
    private val parfaitGroupMemberQueryPort: ParfaitGroupMemberQueryPort,
    private val parfaitQueryPort: ParfaitQueryPort,
    private val parfaitImageQueryPort: ParfaitImageQueryPort,
) : GetParfaitDetailUseCase {
    @Transactional(readOnly = true)
    override fun getDetail(command: GetParfaitDetailCommand): GetTodayParfaitResult {
        if (!parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(command.groupId, command.memberId)) {
            throw ParfaitGroupException(ParfaitGroupError.GROUP_NOT_JOINED)
        }

        val parfait =
            parfaitQueryPort.findByIdAndGroupId(command.parfaitId, command.groupId)
                ?: throw BusinessException(ParfaitErrorCode.PARFAIT_NOT_FOUND)

        val lastClosedDate = parfaitQueryPort.findLastClosedDateByGroupId(command.groupId)

        val groupMembers =
            parfaitGroupMemberQueryPort.findAllByGroupId(command.groupId).map {
                GroupMemberResult(
                    id = requireNotNull(it.id),
                    nickname = it.groupNickname.value,
                    nametagChip = it.nametagChip,
                )
            }

        val images = buildImages(parfait.requireId(), command.memberId)

        val background =
            if (parfait.backgroundType != null && parfait.backgroundValue != null) {
                BackgroundResult(type = parfait.backgroundType, value = parfait.backgroundValue)
            } else {
                null
            }

        return GetTodayParfaitResult(
            parfaitId = parfait.requireId(),
            date = parfait.parfaitDate,
            status = parfait.status,
            lastClosedDate = lastClosedDate,
            groupMembers = groupMembers,
            background = background,
            images = images,
        )
    }

    private fun buildImages(
        parfaitId: Long,
        requesterMemberId: Long,
    ): List<TodayParfaitImageResult>? {
        val parfaitImages = parfaitImageQueryPort.findAllByParfaitId(parfaitId)
        if (parfaitImages.isEmpty()) {
            return null
        }

        val placerIds = parfaitImages.map { it.placedByGroupMemberId }.distinct()
        val placerById =
            parfaitGroupMemberQueryPort.findAllByIds(placerIds).associateBy {
                requireNotNull(it.id)
            }

        return parfaitImages.map { image ->
            val placer = requireNotNull(placerById[image.placedByGroupMemberId])
            TodayParfaitImageResult(
                parfaitImageId = image.requireId(),
                imageId = image.imageMetaId,
                imageUrl = image.imageUrl,
                positionX = image.positionX,
                positionY = image.positionY,
                positionZ = image.positionZ,
                scale = image.scale,
                rotation = image.rotation,
                borderType = image.borderType,
                borderColor = image.borderColor,
                borderWidth = image.borderWidth,
                placedBy =
                    PlacedByResult(
                        groupMemberId = image.placedByGroupMemberId,
                        nickname = placer.groupNickname.value,
                        nametagChip = placer.nametagChip,
                        ownerType = if (placer.memberId == requesterMemberId) OwnerType.ME else OwnerType.OTHER,
                    ),
                createdAt = image.createdAt,
            )
        }
    }
}
