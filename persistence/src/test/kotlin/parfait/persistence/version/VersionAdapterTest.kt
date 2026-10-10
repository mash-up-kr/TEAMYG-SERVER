package parfait.persistence.version

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.transaction.annotation.Transactional
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import parfait.core.exception.BusinessException
import parfait.core.parfait.domain.BackgroundType
import parfait.core.parfait.domain.Parfait
import parfait.core.parfait.exception.ParfaitErrorCode
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.core.parfait.port.out.ParfaitSavePort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupSavePort
import parfait.core.parfaitgroup.domain.InviteCode
import parfait.core.parfaitgroup.domain.ParfaitGroup
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException
import parfait.persistence.TestApplication
import java.time.LocalDate
import kotlin.test.assertFailsWith

@Testcontainers
@SpringBootTest(classes = [TestApplication::class])
class VersionAdapterTest {
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
    private lateinit var parfaitVersionAdapter: ParfaitVersionAdapter

    @Autowired
    private lateinit var parfaitGroupVersionAdapter: ParfaitGroupVersionAdapter

    @Autowired
    private lateinit var groupSavePort: ParfaitGroupSavePort

    @Autowired
    private lateinit var parfaitSavePort: ParfaitSavePort

    @Autowired
    private lateinit var parfaitQueryPort: ParfaitQueryPort

    private fun newGroupId(inviteCode: String): Long =
        groupSavePort
            .save(ParfaitGroup.create(name = "버전테스트", inviteCode = InviteCode.of(inviteCode), memberLimit = 12))
            .requireId()

    private fun newParfaitId(
        groupId: Long,
        date: LocalDate = LocalDate.of(2026, 10, 10),
    ): Long = parfaitSavePort.save(Parfait.createToday(parfaitGroupId = groupId, date = date)).requireId()

    @Test
    fun `새 파르페의 version은 1이고 bump 할 때마다 1씩 오른다`() {
        val parfaitId = newParfaitId(newGroupId("VER001"))

        parfaitVersionAdapter.getVersion(parfaitId) shouldBe 1L
        parfaitVersionAdapter.bump(parfaitId)
        parfaitVersionAdapter.bump(parfaitId)

        parfaitVersionAdapter.getVersion(parfaitId) shouldBe 3L
    }

    @Test
    fun `엔티티 save 로 파르페를 다시 저장해도 version은 덮어써지지 않는다`() {
        val groupId = newGroupId("VER002")
        val parfaitId = newParfaitId(groupId)
        parfaitVersionAdapter.bump(parfaitId)
        val loaded = requireNotNull(parfaitQueryPort.findByGroupIdAndDate(groupId, LocalDate.of(2026, 10, 10)))

        parfaitSavePort.save(loaded.changeBackground(BackgroundType.COLOR, "#FFFFFF"))

        parfaitVersionAdapter.getVersion(parfaitId) shouldBe 2L
    }

    @Test
    fun `존재하지 않는 파르페는 getVersion 과 bump 에서 PARFAIT_NOT_FOUND 를 던진다`() {
        assertFailsWith<BusinessException> { parfaitVersionAdapter.getVersion(999_999L) }
            .errorCode shouldBe ParfaitErrorCode.PARFAIT_NOT_FOUND
        assertFailsWith<BusinessException> { parfaitVersionAdapter.bump(999_999L) }
            .errorCode shouldBe ParfaitErrorCode.PARFAIT_NOT_FOUND
    }

    @Test
    fun `새 그룹의 version은 1이고 bump 할 때마다 1씩 오른다`() {
        val groupId = newGroupId("VER003")

        parfaitGroupVersionAdapter.getVersion(groupId) shouldBe 1L
        parfaitGroupVersionAdapter.bump(groupId)

        parfaitGroupVersionAdapter.getVersion(groupId) shouldBe 2L
    }

    @Test
    fun `존재하지 않는 그룹은 GROUP_NOT_FOUND 를 던진다`() {
        assertFailsWith<ParfaitGroupException> { parfaitGroupVersionAdapter.getVersion(999_999L) }
            .error shouldBe ParfaitGroupError.GROUP_NOT_FOUND
        assertFailsWith<ParfaitGroupException> { parfaitGroupVersionAdapter.bump(999_999L) }
            .error shouldBe ParfaitGroupError.GROUP_NOT_FOUND
    }

    @Test
    fun `getVersion 은 읽기 전용 트랜잭션으로 실행한다`() {
        val long = Long::class.javaPrimitiveType

        ParfaitVersionAdapter::class.java
            .getMethod("getVersion", long)
            .getAnnotation(Transactional::class.java)
            ?.readOnly shouldBe true
        ParfaitGroupVersionAdapter::class.java
            .getMethod("getVersion", long)
            .getAnnotation(Transactional::class.java)
            ?.readOnly shouldBe true
    }
}
