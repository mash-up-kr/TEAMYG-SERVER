package parfait.core.parfaitgroup.application.service

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import parfait.core.member.port.out.MemberQueryPort
import parfait.core.parfait.domain.ParfaitDay
import parfait.core.parfait.port.`in`.EnsureActiveCanvasUseCase
import parfait.core.parfaitgroup.application.port.`in`.ChangeMyParfaitGroupNicknameCommand
import parfait.core.parfaitgroup.application.port.`in`.ChangeMyParfaitGroupNicknameResult
import parfait.core.parfaitgroup.application.port.`in`.ChangeMyParfaitGroupNicknameUseCase
import parfait.core.parfaitgroup.application.port.`in`.CreateParfaitGroupCommand
import parfait.core.parfaitgroup.application.port.`in`.CreateParfaitGroupResult
import parfait.core.parfaitgroup.application.port.`in`.CreateParfaitGroupUseCase
import parfait.core.parfaitgroup.application.port.`in`.GetMyParfaitGroupDetailUseCase
import parfait.core.parfaitgroup.application.port.`in`.GetMyParfaitGroupsUseCase
import parfait.core.parfaitgroup.application.port.`in`.JoinParfaitGroupCommand
import parfait.core.parfaitgroup.application.port.`in`.JoinParfaitGroupResult
import parfait.core.parfaitgroup.application.port.`in`.JoinParfaitGroupUseCase
import parfait.core.parfaitgroup.application.port.`in`.LeaveParfaitGroupCommand
import parfait.core.parfaitgroup.application.port.`in`.LeaveParfaitGroupResult
import parfait.core.parfaitgroup.application.port.`in`.LeaveParfaitGroupUseCase
import parfait.core.parfaitgroup.application.port.`in`.MyParfaitGroupDetailResult
import parfait.core.parfaitgroup.application.port.`in`.MyParfaitGroupResult
import parfait.core.parfaitgroup.application.port.`in`.ParfaitGroupMemberResult
import parfait.core.parfaitgroup.application.port.`in`.PreviewParfaitGroupJoinResult
import parfait.core.parfaitgroup.application.port.`in`.PreviewParfaitGroupJoinUseCase
import parfait.core.parfaitgroup.application.port.`in`.ReportParfaitGroupCommand
import parfait.core.parfaitgroup.application.port.`in`.ReportParfaitGroupResult
import parfait.core.parfaitgroup.application.port.`in`.ReportParfaitGroupUseCase
import parfait.core.parfaitgroup.application.port.out.MyParfaitGroupQueryPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberLeavePort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberSavePort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupQueryPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupReportSavePort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupSavePort
import parfait.core.parfaitgroup.domain.GroupNickname
import parfait.core.parfaitgroup.domain.InviteCode
import parfait.core.parfaitgroup.domain.InviteCodeGenerator
import parfait.core.parfaitgroup.domain.NameTagChipType
import parfait.core.parfaitgroup.domain.ParfaitGroup
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException
import parfait.core.parfaitgroup.domain.ParfaitGroupMember
import parfait.core.parfaitgroup.domain.ParfaitGroupReport

@Service
class ParfaitGroupService(
    private val parfaitGroupQueryPort: ParfaitGroupQueryPort,
    private val parfaitGroupSavePort: ParfaitGroupSavePort,
    private val parfaitGroupMemberQueryPort: ParfaitGroupMemberQueryPort,
    private val parfaitGroupMemberSavePort: ParfaitGroupMemberSavePort,
    private val parfaitGroupMemberLeavePort: ParfaitGroupMemberLeavePort,
    private val parfaitGroupReportSavePort: ParfaitGroupReportSavePort,
    private val myParfaitGroupQueryPort: MyParfaitGroupQueryPort,
    private val memberQueryPort: MemberQueryPort,
    private val inviteCodeGenerator: InviteCodeGenerator,
    private val ensureActiveCanvasUseCase: EnsureActiveCanvasUseCase,
) : PreviewParfaitGroupJoinUseCase,
    JoinParfaitGroupUseCase,
    CreateParfaitGroupUseCase,
    GetMyParfaitGroupsUseCase,
    GetMyParfaitGroupDetailUseCase,
    ChangeMyParfaitGroupNicknameUseCase,
    LeaveParfaitGroupUseCase,
    ReportParfaitGroupUseCase {
    @Transactional(readOnly = true)
    override fun preview(
        memberId: Long,
        inviteCode: String,
    ): PreviewParfaitGroupJoinResult {
        val group = findGroup(InviteCode.of(inviteCode), forUpdate = false)
        validateJoin(group, memberId)
        return PreviewParfaitGroupJoinResult(groupName = group.name.value)
    }

    @Transactional
    override fun join(command: JoinParfaitGroupCommand): JoinParfaitGroupResult {
        val group = findGroup(InviteCode.of(command.inviteCode), forUpdate = true)
        val nickname = validateJoin(group, command.memberId)
        parfaitGroupMemberSavePort.save(
            ParfaitGroupMember.join(
                parfaitGroupId = group.requireId(),
                memberId = command.memberId,
                groupNickname = nickname.value,
                nametagChip = assignNametagChip(group.requireId()),
            ),
        )
        return JoinParfaitGroupResult(
            groupId = group.requireId(),
            groupName = group.name.value,
        )
    }

    @Transactional
    override fun create(command: CreateParfaitGroupCommand): CreateParfaitGroupResult {
        val nickname = GroupNickname.of(command.groupNickname)
        requireMember(command.memberId)
        val inviteCode = generateUniqueInviteCode()
        val savedGroup =
            parfaitGroupSavePort.save(
                ParfaitGroup.create(
                    name = command.groupName,
                    inviteCode = inviteCode,
                    memberLimit = command.memberLimit,
                ),
            )
        ensureActiveCanvasUseCase.ensure(savedGroup.requireId(), ParfaitDay.current())
        parfaitGroupMemberSavePort.save(
            ParfaitGroupMember.join(
                parfaitGroupId = savedGroup.requireId(),
                memberId = command.memberId,
                groupNickname = nickname.value,
                nametagChip = assignNametagChip(savedGroup.requireId()),
            ),
        )
        return CreateParfaitGroupResult(
            groupId = savedGroup.requireId(),
            groupName = savedGroup.name.value,
            inviteCode = savedGroup.inviteCode.value,
            memberLimit = savedGroup.memberLimit.value,
        )
    }

    @Transactional(readOnly = true)
    override fun getAll(memberId: Long): List<MyParfaitGroupResult> =
        myParfaitGroupQueryPort.findAllByMemberId(memberId).map {
            MyParfaitGroupResult(
                groupId = it.groupId,
                groupName = it.groupName,
                recentImageUrl = it.recentImageUrl,
                recentImageUploadedAt = it.recentImageUploadedAt,
                lastPlacedByNametagChip = it.lastPlacedByNametagChip,
            )
        }

    @Transactional(readOnly = true)
    override fun get(
        memberId: Long,
        groupId: Long,
    ): MyParfaitGroupDetailResult {
        val group = findGroupById(groupId)
        val myMembership = findMembership(groupId, memberId)
        return MyParfaitGroupDetailResult(
            groupId = group.requireId(),
            groupName = group.name.value,
            groupNickname = myMembership.groupNickname.value,
            inviteCode = group.inviteCode.value,
            memberLimit = group.memberLimit.value,
            members =
                parfaitGroupMemberQueryPort.findAllByGroupId(groupId).map {
                    ParfaitGroupMemberResult(
                        memberId = it.memberId,
                        groupNickname = it.groupNickname.value,
                        nametagChip = it.nametagChip,
                    )
                },
        )
    }

    @Transactional
    override fun change(command: ChangeMyParfaitGroupNicknameCommand): ChangeMyParfaitGroupNicknameResult {
        findGroupByIdForUpdate(command.groupId)
        val membership = findMembership(command.groupId, command.memberId)
        val changedMembership = membership.changeNickname(command.groupNickname)
        if (changedMembership.groupNickname != membership.groupNickname) {
            parfaitGroupMemberSavePort.save(changedMembership)
        }
        return ChangeMyParfaitGroupNicknameResult(
            groupId = command.groupId,
            groupNickname = changedMembership.groupNickname.value,
        )
    }

    @Transactional
    override fun leave(command: LeaveParfaitGroupCommand): LeaveParfaitGroupResult {
        findGroupByIdForUpdate(command.groupId)
        parfaitGroupMemberLeavePort.leave(findMembership(command.groupId, command.memberId).leave())
        return LeaveParfaitGroupResult(groupId = command.groupId)
    }

    @Transactional
    override fun report(command: ReportParfaitGroupCommand): ReportParfaitGroupResult {
        val report =
            ParfaitGroupReport.create(
                parfaitGroupId = command.groupId,
                reporterMemberId = command.memberId,
                reason = command.reason,
            )
        findGroupByIdForUpdate(command.groupId)
        val membership = findMembership(command.groupId, command.memberId)
        val savedReport = parfaitGroupReportSavePort.save(report)
        parfaitGroupMemberLeavePort.leave(membership.leave())
        return ReportParfaitGroupResult(
            groupId = command.groupId,
            reportId = requireNotNull(savedReport.id),
        )
    }

    private fun findGroup(
        inviteCode: InviteCode,
        forUpdate: Boolean,
    ): ParfaitGroup {
        val group =
            if (forUpdate) {
                parfaitGroupQueryPort.findByInviteCodeForUpdate(inviteCode)
            } else {
                parfaitGroupQueryPort.findByInviteCode(inviteCode)
            }
        return group ?: throw ParfaitGroupException(ParfaitGroupError.INVALID_INVITE_CODE)
    }

    private fun findGroupById(groupId: Long): ParfaitGroup =
        parfaitGroupQueryPort.findById(groupId)
            ?: throw ParfaitGroupException(ParfaitGroupError.GROUP_NOT_FOUND)

    private fun findGroupByIdForUpdate(groupId: Long): ParfaitGroup =
        parfaitGroupQueryPort.findByIdForUpdate(groupId)
            ?: throw ParfaitGroupException(ParfaitGroupError.GROUP_NOT_FOUND)

    private fun validateJoin(
        group: ParfaitGroup,
        memberId: Long,
    ): GroupNickname {
        val groupId = group.requireId()
        group.validateJoin(
            alreadyJoined = parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(groupId, memberId),
            currentMemberCount = parfaitGroupMemberQueryPort.countByGroupId(groupId),
        )
        return GroupNickname.of(requireMemberNickname(memberId))
    }

    private fun requireMemberNickname(memberId: Long): String =
        memberQueryPort.findGlobalNicknameById(memberId)
            ?: throw ParfaitGroupException(ParfaitGroupError.MEMBER_NOT_FOUND)

    private fun requireMember(memberId: Long) {
        if (!memberQueryPort.existsById(memberId)) {
            throw ParfaitGroupException(ParfaitGroupError.MEMBER_NOT_FOUND)
        }
    }

    private fun findMembership(
        groupId: Long,
        memberId: Long,
    ): ParfaitGroupMember =
        parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(groupId, memberId)
            ?: throw ParfaitGroupException(ParfaitGroupError.GROUP_NOT_JOINED)

    private fun generateUniqueInviteCode(): InviteCode {
        while (true) {
            val inviteCode = inviteCodeGenerator.generate()
            if (!parfaitGroupQueryPort.existsByInviteCode(inviteCode)) {
                return inviteCode
            }
        }
    }

    private fun assignNametagChip(groupId: Long): NameTagChipType {
        val occupied =
            parfaitGroupMemberQueryPort
                .findAllByGroupId(groupId)
                .mapNotNull { it.nametagChip }
                .toSet()
        return NameTagChipType.assignRandom(occupied)
    }
}
