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
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberLeavePort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort

@Service
class MemberService(
    private val memberNicknameUpdatePort: MemberNicknameUpdatePort,
    private val memberQueryPort: MemberQueryPort,
    private val memberDeletePort: MemberDeletePort,
    private val parfaitGroupMemberQueryPort: ParfaitGroupMemberQueryPort,
    private val parfaitGroupMemberLeavePort: ParfaitGroupMemberLeavePort,
    private val tokenDeletePort: TokenDeletePort,
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
        parfaitGroupMemberQueryPort.findAllMembershipsByMemberId(memberId).forEach {
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
