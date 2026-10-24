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
import parfait.core.parfaittext.port.`in`.DeleteParfaitTextCommand
import parfait.core.parfaittext.port.`in`.DeleteParfaitTextUseCase
import parfait.core.parfaittext.port.out.ParfaitTextDeletePort
import parfait.core.parfaittext.port.out.ParfaitTextQueryPort

@Service
class DeleteParfaitTextService(
    private val parfaitGroupMemberQueryPort: ParfaitGroupMemberQueryPort,
    private val parfaitQueryPort: ParfaitQueryPort,
    private val parfaitTextQueryPort: ParfaitTextQueryPort,
    private val parfaitTextDeletePort: ParfaitTextDeletePort,
    private val parfaitVersionService: ParfaitVersionService,
) : DeleteParfaitTextUseCase {
    @Transactional
    override fun delete(command: DeleteParfaitTextCommand) {
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

        // 배치와 동일하게 부모 parfait 행의 락을 자식 변경보다 먼저 얻는다.
        parfaitVersionService.bump(command.parfaitId)
        parfaitTextDeletePort.deleteById(parfaitText.requireId())
    }
}
