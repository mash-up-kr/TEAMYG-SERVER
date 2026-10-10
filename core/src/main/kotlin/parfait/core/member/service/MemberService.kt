package parfait.core.member.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import parfait.core.auth.port.out.TokenDeletePort
import parfait.core.exception.BusinessException
import parfait.core.member.domain.GlobalNickname
import parfait.core.member.exception.MemberErrorCode
import parfait.core.member.port.`in`.ChangeGlobalNicknameResult
import parfait.core.member.port.`in`.ChangeGlobalNicknameUseCase
import parfait.core.member.port.`in`.GetMyAccountUseCase
import parfait.core.member.port.`in`.MyAccountResult
import parfait.core.member.port.`in`.WithdrawUseCase
import parfait.core.member.port.out.MemberDeletePort
import parfait.core.member.port.out.MemberNicknameUpdatePort
import parfait.core.member.port.out.MemberQueryPort
import parfait.core.notification.port.out.DeviceTokenDeletePort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberLeavePort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitgroup.application.service.ParfaitGroupVersionService

@Service
class MemberService(
    private val memberNicknameUpdatePort: MemberNicknameUpdatePort,
    private val memberQueryPort: MemberQueryPort,
    private val memberDeletePort: MemberDeletePort,
    private val parfaitGroupMemberQueryPort: ParfaitGroupMemberQueryPort,
    private val parfaitGroupMemberLeavePort: ParfaitGroupMemberLeavePort,
    private val tokenDeletePort: TokenDeletePort,
    private val deviceTokenDeletePort: DeviceTokenDeletePort,
    private val parfaitGroupVersionService: ParfaitGroupVersionService,
) : ChangeGlobalNicknameUseCase,
    WithdrawUseCase,
    GetMyAccountUseCase {
    private val log = LoggerFactory.getLogger(MemberService::class.java)

    @Transactional
    override fun change(
        memberId: Long,
        nickname: String,
    ): ChangeGlobalNicknameResult {
        val globalNickname = GlobalNickname.of(nickname)
        memberNicknameUpdatePort.updateGlobalNickname(memberId, globalNickname.value)
        return ChangeGlobalNicknameResult(globalNickname.value)
    }

    @Transactional
    override fun withdraw(memberId: Long) {
        if (!memberQueryPort.existsById(memberId)) return
        memberDeletePort.deleteById(memberId)
        // 남은 멤버가 탈퇴자가 포함된 옛 멤버 목록을 304로 계속 받지 않도록 그룹 version 을 올린다.
        // 그룹 id 오름차순으로 처리해, 겹치는 그룹들에서 두 사람이 동시에 탈퇴해도 parfait_group 행 락을
        // 같은 순서로 잡게 한다(순서가 다르면 교차 대기로 데드락이 날 수 있다).
        // 그룹 나가기(leave)와 같이 부모 parfait_group 행 락을 멤버십 변경보다 먼저 잡는다.
        parfaitGroupMemberQueryPort
            .findAllMembershipsByMemberId(memberId)
            .sortedBy { it.parfaitGroupId }
            .forEach {
                parfaitGroupVersionService.bumpOnMemberLeft(it.parfaitGroupId, memberId)
                parfaitGroupMemberLeavePort.leave(it.leave())
            }
        // DB 트랜잭션이 실제로 커밋된 뒤에만 Redis를 정리한다. runCatching만으로는 메서드 본문에서
        // 던져지는 예외만 잡을 뿐, 이후 커밋 자체가 실패하는 경우(DB 트랜잭션은 롤백됐는데
        // Redis는 이미 지워진 상태)를 막지 못하기 때문이다.
        TransactionSynchronizationManager.registerSynchronization(
            object : TransactionSynchronization {
                override fun afterCommit() {
                    runCatching { tokenDeletePort.deleteAllByMemberId(memberId) }
                        .onFailure { log.warn("탈퇴 후 refresh token 정리 실패: memberId={}", memberId, it) }
                    runCatching { deviceTokenDeletePort.deleteAllByMemberId(memberId) }
                        .onFailure { log.warn("탈퇴 후 기기 토큰 정리 실패: memberId={}", memberId, it) }
                }
            },
        )
    }

    override fun getMyAccount(memberId: Long): MyAccountResult {
        val account =
            memberQueryPort.findAccountById(memberId)
                ?: throw BusinessException(MemberErrorCode.MEMBER_NOT_FOUND)
        return MyAccountResult(memberId, account.provider, account.nickname)
    }
}
