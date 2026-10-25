package parfait.core.parfaittext.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import parfait.core.exception.BusinessException
import parfait.core.notification.domain.TextPlacedPayload
import parfait.core.notification.service.TextPlacedNotifier
import parfait.core.parfait.domain.OwnerType
import parfait.core.parfait.domain.ParfaitStatus
import parfait.core.parfait.exception.ParfaitErrorCode
import parfait.core.parfait.port.`in`.PlacedByResult
import parfait.core.parfait.port.`in`.TodayParfaitTextResult
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.core.parfait.service.ParfaitVersionService
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException
import parfait.core.parfaittext.domain.ParfaitText
import parfait.core.parfaittext.port.`in`.PlaceParfaitTextCommand
import parfait.core.parfaittext.port.`in`.PlaceParfaitTextUseCase
import parfait.core.parfaittext.port.out.ParfaitTextSavePort

@Service
class PlaceParfaitTextService(
    private val parfaitGroupMemberQueryPort: ParfaitGroupMemberQueryPort,
    private val parfaitQueryPort: ParfaitQueryPort,
    private val parfaitTextSavePort: ParfaitTextSavePort,
    private val parfaitVersionService: ParfaitVersionService,
    private val textPlacedNotifier: TextPlacedNotifier,
) : PlaceParfaitTextUseCase {
    @Transactional
    override fun place(command: PlaceParfaitTextCommand): TodayParfaitTextResult {
        val groupMember =
            parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(command.groupId, command.memberId)
                ?: throw ParfaitGroupException(ParfaitGroupError.GROUP_NOT_JOINED)
        val parfait =
            parfaitQueryPort.findByIdAndGroupId(command.parfaitId, command.groupId)
                ?: throw BusinessException(ParfaitErrorCode.PARFAIT_NOT_FOUND)
        if (parfait.status != ParfaitStatus.ACTIVE) {
            throw BusinessException(ParfaitErrorCode.PARFAIT_ALREADY_CLOSED)
        }

        val text =
            ParfaitText.place(
                parfaitId = command.parfaitId,
                placedByGroupMemberId = requireNotNull(groupMember.id),
                text = command.text,
                positionX = command.positionX,
                positionY = command.positionY,
                positionZ = command.positionZ,
                scale = command.scale,
                rotation = command.rotation,
                color = command.color,
                fontSize = command.fontSize,
                isFill = command.isFill,
                isStroke = command.isStroke,
            )

        // 이미지 배치와 동일하게 FK의 공유 락을 얻기 전에 부모 행을 갱신해 락 승격 데드락을 방지한다.
        parfaitVersionService.bump(command.parfaitId)
        val saved = parfaitTextSavePort.save(text)

        textPlacedNotifier.notify(
            TextPlacedPayload(
                groupId = command.groupId,
                parfaitId = command.parfaitId,
                parfaitDate = parfait.parfaitDate,
                actorMemberId = command.memberId,
                text = command.text,
            ),
            saved.requireId(),
        )

        return TodayParfaitTextResult.from(
            saved,
            PlacedByResult(
                groupMemberId = requireNotNull(groupMember.id),
                nickname = groupMember.groupNickname.value,
                nametagChip = groupMember.nametagChip,
                ownerType = OwnerType.ME,
            ),
        )
    }
}
