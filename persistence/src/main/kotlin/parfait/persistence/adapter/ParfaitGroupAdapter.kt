package parfait.persistence.adapter

import org.springframework.stereotype.Component
import parfait.core.parfaitgroup.application.port.out.MyParfaitGroupQueryPort
import parfait.core.parfaitgroup.application.port.out.MyParfaitGroupSummary
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberLeavePort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberSavePort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupQueryPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupSavePort
import parfait.core.parfaitgroup.domain.GroupNickname
import parfait.core.parfaitgroup.domain.InviteCode
import parfait.core.parfaitgroup.domain.ParfaitGroup
import parfait.core.parfaitgroup.domain.ParfaitGroupMember
import parfait.persistence.repository.ParfaitGroupMemberRepository
import parfait.persistence.repository.ParfaitGroupRepository
import parfait.persistence.entity.ParfaitGroup as ParfaitGroupEntity
import parfait.persistence.entity.ParfaitGroupMember as ParfaitGroupMemberEntity

@Component
class ParfaitGroupAdapter(
    private val parfaitGroupRepository: ParfaitGroupRepository,
    private val parfaitGroupMemberRepository: ParfaitGroupMemberRepository,
) : ParfaitGroupQueryPort,
    ParfaitGroupSavePort,
    ParfaitGroupMemberQueryPort,
    ParfaitGroupMemberSavePort,
    ParfaitGroupMemberLeavePort,
    MyParfaitGroupQueryPort {
    override fun findById(groupId: Long): ParfaitGroup? =
        parfaitGroupRepository.findById(groupId).orElse(null)?.toDomain()

    override fun findByIdForUpdate(groupId: Long): ParfaitGroup? =
        parfaitGroupRepository.findByIdForUpdate(groupId)?.toDomain()

    override fun findByInviteCode(inviteCode: InviteCode): ParfaitGroup? =
        parfaitGroupRepository.findByInviteCode(inviteCode.value)?.toDomain()

    override fun findByInviteCodeForUpdate(inviteCode: InviteCode): ParfaitGroup? =
        parfaitGroupRepository.findByInviteCodeForUpdate(inviteCode.value)?.toDomain()

    override fun existsByInviteCode(inviteCode: InviteCode): Boolean =
        parfaitGroupRepository.existsByInviteCode(inviteCode.value)

    override fun save(group: ParfaitGroup): ParfaitGroup = parfaitGroupRepository.save(group.toEntity()).toDomain()

    override fun existsByGroupIdAndMemberId(
        groupId: Long,
        memberId: Long,
    ): Boolean = parfaitGroupMemberRepository.existsByParfaitGroupIdAndMemberId(groupId, memberId)

    override fun findByGroupIdAndMemberId(
        groupId: Long,
        memberId: Long,
    ): ParfaitGroupMember? =
        parfaitGroupMemberRepository.findByParfaitGroupIdAndMemberIdAndLeftAtIsNull(groupId, memberId)?.toDomain()

    override fun findAllByGroupId(groupId: Long): List<ParfaitGroupMember> =
        parfaitGroupMemberRepository.findAllByParfaitGroupIdAndLeftAtIsNullOrderByJoinedAtAscIdAsc(groupId).map {
            it.toDomain()
        }

    override fun existsByGroupIdAndNickname(
        groupId: Long,
        nickname: GroupNickname,
    ): Boolean =
        parfaitGroupMemberRepository.existsByParfaitGroupIdAndGroupNicknameAndLeftAtIsNull(groupId, nickname.value)

    override fun countByGroupId(groupId: Long): Int =
        Math.toIntExact(parfaitGroupMemberRepository.countByParfaitGroupIdAndLeftAtIsNull(groupId))

    override fun save(groupMember: ParfaitGroupMember): ParfaitGroupMember =
        parfaitGroupMemberRepository.save(groupMember.toEntity()).toDomain()

    override fun leave(groupMember: ParfaitGroupMember) {
        parfaitGroupMemberRepository.save(groupMember.toEntity())
    }

    override fun findAllByMemberId(memberId: Long): List<MyParfaitGroupSummary> =
        parfaitGroupMemberRepository.findMyGroupSummaries(memberId).map {
            MyParfaitGroupSummary(
                groupId = it.groupId,
                groupName = it.groupName,
                recentImageUrl = it.recentImageUrl,
                recentImageUploadedAt = it.recentImageUploadedAt,
            )
        }

    private fun ParfaitGroup.toEntity(): ParfaitGroupEntity =
        ParfaitGroupEntity(
            name = name.value,
            inviteCode = inviteCode.value,
            memberLimit = memberLimit.value,
            createdAt = createdAt,
            updatedAt = updatedAt,
            id = id,
        )

    private fun ParfaitGroupEntity.toDomain(): ParfaitGroup =
        ParfaitGroup.reconstitute(
            id = requireNotNull(id),
            name = name,
            inviteCode = inviteCode,
            memberLimit = memberLimit,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )

    private fun ParfaitGroupMember.toEntity(): ParfaitGroupMemberEntity =
        ParfaitGroupMemberEntity(
            parfaitGroupId = parfaitGroupId,
            memberId = memberId,
            groupNickname = groupNickname.value,
            joinedAt = joinedAt,
            leftAt = leftAt,
            id = id,
        )

    private fun ParfaitGroupMemberEntity.toDomain(): ParfaitGroupMember =
        ParfaitGroupMember.reconstitute(
            id = requireNotNull(id),
            parfaitGroupId = parfaitGroupId,
            memberId = memberId,
            groupNickname = groupNickname,
            joinedAt = joinedAt,
            leftAt = leftAt,
        )
}
