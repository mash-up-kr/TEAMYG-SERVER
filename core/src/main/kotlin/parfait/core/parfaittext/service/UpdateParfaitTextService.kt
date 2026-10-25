package parfait.core.parfaittext.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import parfait.core.exception.BusinessException
import parfait.core.parfait.domain.ParfaitStatus
import parfait.core.parfait.exception.ParfaitErrorCode
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.core.parfait.service.ParfaitVersionService
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaittext.exception.ParfaitTextErrorCode
import parfait.core.parfaittext.port.`in`.UpdateParfaitTextCommand
import parfait.core.parfaittext.port.`in`.UpdateParfaitTextResult
import parfait.core.parfaittext.port.`in`.UpdateParfaitTextUseCase
import parfait.core.parfaittext.port.out.ParfaitTextQueryPort
import parfait.core.parfaittext.port.out.ParfaitTextSavePort

@Service
class UpdateParfaitTextService(
    private val parfaitGroupMemberQueryPort: ParfaitGroupMemberQueryPort,
    private val parfaitQueryPort: ParfaitQueryPort,
    private val parfaitTextQueryPort: ParfaitTextQueryPort,
    private val parfaitTextSavePort: ParfaitTextSavePort,
    private val parfaitVersionService: ParfaitVersionService,
) : UpdateParfaitTextUseCase {
    @Transactional
    override fun update(command: UpdateParfaitTextCommand): UpdateParfaitTextResult {
        val parfaitText =
            parfaitTextQueryPort
                .findById(command.parfaitTextId)
                ?.takeIf { it.parfaitId == command.parfaitId }
                ?: throw BusinessException(ParfaitTextErrorCode.PARFAIT_TEXT_NOT_FOUND)

        val groupMember = parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(command.groupId, command.memberId)
        if (groupMember == null || groupMember.id != parfaitText.placedByGroupMemberId) {
            throw BusinessException(ParfaitTextErrorCode.PARFAIT_TEXT_NOT_OWNED)
        }

        val parfait =
            parfaitQueryPort.findByIdAndGroupId(command.parfaitId, command.groupId)
                ?: throw BusinessException(ParfaitErrorCode.PARFAIT_NOT_FOUND)
        if (parfait.status != ParfaitStatus.ACTIVE) {
            throw BusinessException(ParfaitErrorCode.PARFAIT_ALREADY_CLOSED)
        }

        val updated =
            parfaitText.update(
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
        // 배치와 동일하게 부모 parfait 행의 락을 자식 변경보다 먼저 얻는다.
        parfaitVersionService.bump(command.parfaitId)
        val saved = parfaitTextSavePort.save(updated)
        return UpdateParfaitTextResult(
            parfaitTextId = saved.requireId(),
            text = saved.text,
            positionX = saved.positionX,
            positionY = saved.positionY,
            positionZ = saved.positionZ,
            scale = saved.scale,
            rotation = saved.rotation,
            color = saved.color,
            fontSize = saved.fontSize,
            isFill = saved.isFill,
            isStroke = saved.isStroke,
        )
    }
}
