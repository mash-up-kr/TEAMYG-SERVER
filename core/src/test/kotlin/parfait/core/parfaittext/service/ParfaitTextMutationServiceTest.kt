package parfait.core.parfaittext.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import parfait.core.exception.BusinessException
import parfait.core.parfait.domain.Parfait
import parfait.core.parfait.domain.ParfaitStatus
import parfait.core.parfait.exception.ParfaitErrorCode
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.core.parfait.service.ParfaitVersionService
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitgroup.domain.ParfaitGroupMember
import parfait.core.parfaittext.domain.ParfaitText
import parfait.core.parfaittext.exception.ParfaitTextErrorCode
import parfait.core.parfaittext.port.`in`.DeleteParfaitTextCommand
import parfait.core.parfaittext.port.`in`.UpdateParfaitTextCommand
import parfait.core.parfaittext.port.out.ParfaitTextDeletePort
import parfait.core.parfaittext.port.out.ParfaitTextQueryPort
import parfait.core.parfaittext.port.out.ParfaitTextSavePort
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertFailsWith

class ParfaitTextMutationServiceTest {
    private val members = mockk<ParfaitGroupMemberQueryPort>()
    private val parfaits = mockk<ParfaitQueryPort>()
    private val texts = mockk<ParfaitTextQueryPort>()
    private val saves = mockk<ParfaitTextSavePort>()
    private val deletes = mockk<ParfaitTextDeletePort>()
    private val versions = mockk<ParfaitVersionService>(relaxed = true)
    private val updateService = UpdateParfaitTextService(members, parfaits, texts, saves, versions)
    private val deleteService = DeleteParfaitTextService(members, parfaits, texts, deletes, versions)
    private val createdAt = LocalDateTime.of(2026, 10, 20, 14, 30)
    private val existing =
        ParfaitText.reconstitute(
            id = 301L,
            parfaitId = 5L,
            placedByGroupMemberId = 10L,
            text = "원래 텍스트",
            positionX = 120.5,
            positionY = 340.2,
            positionZ = 2,
            scale = 1.5,
            rotation = -30.0,
            color = "#FF5733",
            fontSize = 16.0,
            createdAt = createdAt,
            updatedAt = createdAt,
        )
    private val command = UpdateParfaitTextCommand(memberId = 42L, groupId = 1L, parfaitId = 5L, parfaitTextId = 301L)

    enum class Action {
        UPDATE,
        DELETE,
    }

    private fun execute(
        action: Action,
        parfaitId: Long = 5L,
    ) {
        when (action) {
            Action.UPDATE -> updateService.update(command.copy(parfaitId = parfaitId))
            Action.DELETE -> deleteService.delete(DeleteParfaitTextCommand(42L, 1L, parfaitId, 301L))
        }
    }

    private fun parfait(status: ParfaitStatus): Parfait =
        Parfait.reconstitute(
            id = 5L,
            parfaitGroupId = 1L,
            parfaitDate = LocalDate.of(2026, 10, 20),
            status = status,
            backgroundType = null,
            backgroundValue = null,
            createdAt = createdAt,
            updatedAt = createdAt,
        )

    @BeforeEach
    fun setUp() {
        every { texts.findById(301L) } returns existing
        every { members.findByGroupIdAndMemberId(1L, 42L) } returns
            ParfaitGroupMember.reconstitute(
                id = 10L,
                parfaitGroupId = 1L,
                memberId = 42L,
                groupNickname = "연경이",
                joinedAt = createdAt,
            )
        every { parfaits.findByIdAndGroupId(5L, 1L) } returns parfait(ParfaitStatus.ACTIVE)
        every { saves.save(any()) } answers { firstArg() }
        every { deletes.deleteById(any()) } returns Unit
    }

    @Test
    fun `스타일을 수정하고 내용과 배치자 및 생성 시각은 유지한다`() {
        val result =
            updateService.update(
                command.copy(
                    positionX = 10.0,
                    positionY = 20.0,
                    positionZ = 0,
                    scale = 2.0,
                    rotation = 0.0,
                    color = "#FFFFFF",
                    fontSize = 24.0,
                    isFill = false,
                    isStroke = true,
                ),
            )

        result.parfaitTextId shouldBe 301L
        result.text shouldBe existing.text
        result.positionX shouldBe 10.0
        result.positionY shouldBe 20.0
        result.positionZ shouldBe 0
        result.scale shouldBe 2.0
        result.rotation shouldBe 0.0
        result.color shouldBe "#FFFFFF"
        result.fontSize shouldBe 24.0
        result.isFill shouldBe false
        result.isStroke shouldBe true
        verify {
            saves.save(
                match {
                    it.id == existing.id &&
                        it.parfaitId == existing.parfaitId &&
                        it.placedByGroupMemberId == existing.placedByGroupMemberId &&
                        it.createdAt == createdAt &&
                        it.updatedAt.isAfter(createdAt)
                },
            )
        }
        verifyOrder {
            versions.bump(5L)
            saves.save(any())
        }
        verify(exactly = 1) { versions.bump(5L) }
    }

    @Test
    fun `지정한 필드만 수정하고 생략한 값은 유지한다`() {
        val result = updateService.update(command.copy(positionX = 0.0))

        result.text shouldBe existing.text
        result.positionX shouldBe 0.0
        result.positionY shouldBe existing.positionY
        result.positionZ shouldBe existing.positionZ
        result.scale shouldBe existing.scale
        result.rotation shouldBe existing.rotation
        result.color shouldBe existing.color
        result.fontSize shouldBe existing.fontSize
        result.isFill shouldBe existing.isFill
        result.isStroke shouldBe existing.isStroke
    }

    @Test
    fun `모든 필드가 null이면 이미지 수정처럼 기존 값을 유지한다`() {
        val result = updateService.update(command)

        result.text shouldBe existing.text
        result.positionX shouldBe existing.positionX
        result.fontSize shouldBe existing.fontSize
    }

    @Test
    fun `본인이 배치한 텍스트를 삭제하기 전에 캔버스 버전을 갱신한다`() {
        execute(Action.DELETE)

        verifyOrder {
            versions.bump(5L)
            deletes.deleteById(301L)
        }
        verify(exactly = 1) { versions.bump(5L) }
        verify(exactly = 1) { deletes.deleteById(301L) }
        verify(exactly = 0) { saves.save(any()) }
    }

    @ParameterizedTest
    @EnumSource(Action::class)
    fun `텍스트가 없으면 변경을 거부한다`(action: Action) {
        every { texts.findById(301L) } returns null

        val exception = assertFailsWith<BusinessException> { execute(action) }

        exception.errorCode shouldBe ParfaitTextErrorCode.PARFAIT_TEXT_NOT_FOUND
        verifyNoWrites()
    }

    @ParameterizedTest
    @EnumSource(Action::class)
    fun `경로와 다른 파르페에 속한 텍스트는 변경을 거부한다`(action: Action) {
        val exception = assertFailsWith<BusinessException> { execute(action, parfaitId = 999L) }

        exception.errorCode shouldBe ParfaitTextErrorCode.PARFAIT_TEXT_NOT_FOUND
        verifyNoWrites()
    }

    @ParameterizedTest
    @EnumSource(Action::class)
    fun `다른 그룹원의 텍스트는 변경을 거부한다`(action: Action) {
        every { members.findByGroupIdAndMemberId(1L, 42L) } returns
            ParfaitGroupMember.reconstitute(
                id = 11L,
                parfaitGroupId = 1L,
                memberId = 42L,
                groupNickname = "다른 멤버",
                joinedAt = createdAt,
            )

        val exception = assertFailsWith<BusinessException> { execute(action) }

        exception.errorCode shouldBe ParfaitTextErrorCode.PARFAIT_TEXT_NOT_OWNED
        verifyNoWrites()
    }

    @ParameterizedTest
    @EnumSource(Action::class)
    fun `그룹을 나간 작성자는 변경을 거부한다`(action: Action) {
        every { members.findByGroupIdAndMemberId(1L, 42L) } returns null

        val exception = assertFailsWith<BusinessException> { execute(action) }

        exception.errorCode shouldBe ParfaitTextErrorCode.PARFAIT_TEXT_NOT_OWNED
        verifyNoWrites()
    }

    @ParameterizedTest
    @EnumSource(Action::class)
    fun `파르페가 해당 그룹에 없으면 변경을 거부한다`(action: Action) {
        every { parfaits.findByIdAndGroupId(5L, 1L) } returns null

        val exception = assertFailsWith<BusinessException> { execute(action) }

        exception.errorCode shouldBe ParfaitErrorCode.PARFAIT_NOT_FOUND
        verifyNoWrites()
    }

    @ParameterizedTest
    @EnumSource(Action::class)
    fun `마감 또는 빈 상태로 종료된 파르페는 변경을 거부한다`(action: Action) {
        listOf(ParfaitStatus.CLOSED, ParfaitStatus.EMPTY).forEach { status ->
            every { parfaits.findByIdAndGroupId(5L, 1L) } returns parfait(status)
            val exception = assertFailsWith<BusinessException> { execute(action) }
            exception.errorCode shouldBe ParfaitErrorCode.PARFAIT_ALREADY_CLOSED
        }
        verifyNoWrites()
    }

    @ParameterizedTest
    @CsvSource("true,true", "true,false", "false,true", "false,false")
    fun `채움과 외곽선은 각각 독립적으로 변경할 수 있다`(
        isFill: Boolean,
        isStroke: Boolean,
    ) {
        every { texts.findById(301L) } returns existing.update(isFill = false, isStroke = true)

        val result = updateService.update(command.copy(isFill = isFill, isStroke = isStroke))

        result.isFill shouldBe isFill
        result.isStroke shouldBe isStroke
        result.text shouldBe existing.text
    }

    @Test
    fun `잘못된 스타일은 버전을 올리거나 저장하지 않는다`() {
        listOf(
            command.copy(color = " "),
            command.copy(color = "a".repeat(21)),
            command.copy(scale = 0.0),
            command.copy(scale = -1.0),
            command.copy(scale = Double.POSITIVE_INFINITY),
            command.copy(fontSize = 0.0),
            command.copy(fontSize = -1.0),
            command.copy(fontSize = Double.NaN),
            command.copy(positionX = Double.NaN),
            command.copy(positionY = Double.NEGATIVE_INFINITY),
            command.copy(rotation = Double.POSITIVE_INFINITY),
        ).forEach { invalid ->
            val exception = assertFailsWith<BusinessException> { updateService.update(invalid) }
            exception.errorCode shouldBe ParfaitTextErrorCode.INVALID_TEXT_STYLE
        }
        verifyNoWrites()
    }

    private fun verifyNoWrites() {
        verify(exactly = 0) { versions.bump(any()) }
        verify(exactly = 0) { saves.save(any()) }
        verify(exactly = 0) { deletes.deleteById(any()) }
    }
}
