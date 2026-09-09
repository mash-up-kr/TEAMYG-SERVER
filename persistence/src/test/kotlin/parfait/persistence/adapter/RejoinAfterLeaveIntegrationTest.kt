package parfait.persistence.adapter

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import parfait.core.member.port.out.MemberQueryPort
import parfait.core.parfaitgroup.application.port.`in`.JoinParfaitGroupCommand
import parfait.core.parfaitgroup.application.port.`in`.LeaveParfaitGroupCommand
import parfait.core.parfaitgroup.application.service.ParfaitGroupService
import parfait.core.parfaitgroup.domain.InviteCodeGenerator
import parfait.persistence.TestApplication
import parfait.persistence.entity.LoginProvider
import parfait.persistence.entity.Member
import parfait.persistence.repository.MemberRepository
import parfait.persistence.repository.ParfaitGroupMemberRepository
import parfait.persistence.repository.ParfaitGroupRepository
import parfait.persistence.entity.ParfaitGroup as ParfaitGroupEntity

@Testcontainers
@SpringBootTest(classes = [TestApplication::class])
class RejoinAfterLeaveIntegrationTest {
    companion object {
        @Container
        @JvmStatic
        val mysql = MySQLContainer("mysql:8.4")

        @DynamicPropertySource
        @JvmStatic
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { mysql.jdbcUrl }
            registry.add("spring.datasource.username") { mysql.username }
            registry.add("spring.datasource.password") { mysql.password }
        }
    }

    @Autowired
    private lateinit var parfaitGroupRepository: ParfaitGroupRepository

    @Autowired
    private lateinit var parfaitGroupMemberRepository: ParfaitGroupMemberRepository

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @Test
    fun `그룹을 나간 멤버는 같은 그룹에 다시 참여할 수 있다`() {
        val transactionTemplate = TransactionTemplate(transactionManager)
        val groupAdapter = ParfaitGroupAdapter(parfaitGroupRepository, parfaitGroupMemberRepository)
        val member =
            memberRepository.save(
                Member(
                    loginProvider = LoginProvider.KAKAO,
                    providerUserId = "rejoin-test-member",
                    globalNickname = "재참여자",
                ),
            )
        val memberId = requireNotNull(member.id)
        val memberQueryPort =
            mockk<MemberQueryPort> {
                every { findGlobalNicknameById(memberId) } returns "재참여자"
            }
        val service =
            ParfaitGroupService(
                parfaitGroupQueryPort = groupAdapter,
                parfaitGroupSavePort = groupAdapter,
                parfaitGroupMemberQueryPort = groupAdapter,
                parfaitGroupMemberSavePort = groupAdapter,
                parfaitGroupMemberLeavePort = groupAdapter,
                parfaitGroupReportSavePort = mockk(relaxed = true),
                myParfaitGroupQueryPort = groupAdapter,
                memberQueryPort = memberQueryPort,
                inviteCodeGenerator = InviteCodeGenerator(),
                ensureActiveCanvasUseCase = mockk(relaxed = true),
            )
        val group =
            parfaitGroupRepository.save(
                ParfaitGroupEntity(name = "재참여테스트그룹", inviteCode = "REJOIN", memberLimit = 12),
            )

        transactionTemplate.execute {
            service.join(JoinParfaitGroupCommand(memberId = memberId, inviteCode = "REJOIN"))
        }
        transactionTemplate.execute {
            service.leave(LeaveParfaitGroupCommand(groupId = requireNotNull(group.id), memberId = memberId))
        }
        val rejoinResult =
            transactionTemplate.execute {
                service.join(JoinParfaitGroupCommand(memberId = memberId, inviteCode = "REJOIN"))
            }

        requireNotNull(rejoinResult).groupId shouldBe group.id
        parfaitGroupMemberRepository
            .findByParfaitGroupIdAndMemberIdAndLeftAtIsNull(requireNotNull(group.id), memberId) shouldNotBe null
    }
}
