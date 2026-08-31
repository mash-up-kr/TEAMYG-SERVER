package parfait.core.parfaitimage.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import parfait.core.exception.BusinessException
import parfait.core.parfait.domain.Parfait
import parfait.core.parfait.domain.ParfaitStatus
import parfait.core.parfait.exception.ParfaitErrorCode
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitgroup.domain.ParfaitGroupMember
import parfait.core.parfaitimage.domain.BorderType
import parfait.core.parfaitimage.domain.ParfaitImage
import parfait.core.parfaitimage.exception.ParfaitImageErrorCode
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageItemCommand
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImagesCommand
import parfait.core.parfaitimage.port.out.ParfaitImageQueryPort
import parfait.core.parfaitimage.port.out.ParfaitImageSavePort
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertFailsWith

class UpdateParfaitImagesServiceTest {
    private val parfaitGroupMemberQueryPort = mockk<ParfaitGroupMemberQueryPort>()
    private val parfaitQueryPort = mockk<ParfaitQueryPort>()
    private val parfaitImageQueryPort = mockk<ParfaitImageQueryPort>()
    private val parfaitImageSavePort = mockk<ParfaitImageSavePort>()
    private val service =
        UpdateParfaitImagesService(
            parfaitGroupMemberQueryPort = parfaitGroupMemberQueryPort,
            parfaitQueryPort = parfaitQueryPort,
            parfaitImageQueryPort = parfaitImageQueryPort,
            parfaitImageSavePort = parfaitImageSavePort,
        )

    private val owner =
        ParfaitGroupMember.reconstitute(
            id = 10L,
            parfaitGroupId = 1L,
            memberId = 42L,
            groupNickname = "연경이",
            joinedAt = LocalDateTime.now(),
        )

    private fun existingImage(
        id: Long,
        placedByGroupMemberId: Long = 10L,
        parfaitId: Long = 5L,
    ): ParfaitImage =
        ParfaitImage.reconstitute(
            id = id,
            parfaitId = parfaitId,
            imageMetaId = 77L + id,
            placedByGroupMemberId = placedByGroupMemberId,
            imageUrl = "https://s3.example/nukki/user42/$id.png",
            positionX = 0.0,
            positionY = 0.0,
            positionZ = 0,
            scale = 1.0,
            rotation = 0.0,
            borderType = BorderType.NONE,
            borderColor = null,
            borderWidth = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

    private fun activeParfait(status: ParfaitStatus = ParfaitStatus.ACTIVE): Parfait =
        Parfait.reconstitute(
            id = 5L,
            parfaitGroupId = 1L,
            parfaitDate = LocalDate.of(2026, 7, 9),
            status = status,
            backgroundType = null,
            backgroundValue = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

    private fun command(items: List<UpdateParfaitImageItemCommand>) =
        UpdateParfaitImagesCommand(
            memberId = 42L,
            groupId = 1L,
            parfaitId = 5L,
            items = items,
        )

    @Test
    fun `여러 토핑을 한 번에 요청하면 API 호출 한 번으로 전부 갱신한다`() {
        every { parfaitImageQueryPort.findAllByIds(listOf(201L, 202L)) } returns
            listOf(existingImage(201L), existingImage(202L))
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns owner
        every { parfaitQueryPort.findByIdAndGroupId(5L, 1L) } returns activeParfait()
        every { parfaitImageSavePort.saveAll(any()) } answers { firstArg() }

        val result =
            service.updateAll(
                command(
                    listOf(
                        UpdateParfaitImageItemCommand(
                            parfaitImageId = 201L,
                            positionX = 200.0,
                            positionY = 400.0,
                            positionZ = null,
                            scale = 1.5,
                            rotation = 45.0,
                        ),
                        UpdateParfaitImageItemCommand(
                            parfaitImageId = 202L,
                            positionX = 10.0,
                            positionY = 20.0,
                            positionZ = 2,
                            scale = null,
                            rotation = null,
                        ),
                    ),
                ),
            )

        result.size shouldBe 2
        result[0].parfaitImageId shouldBe 201L
        result[0].positionX shouldBe 200.0
        result[0].rotation shouldBe 45.0
        result[1].parfaitImageId shouldBe 202L
        result[1].positionX shouldBe 10.0
        result[1].positionZ shouldBe 2
    }

    @Test
    fun `항목이 비어있으면 조회·저장 없이 빈 목록을 반환한다`() {
        val result = service.updateAll(command(emptyList()))

        result shouldBe emptyList()
    }

    @Test
    fun `그룹 소속의 파르페가 아니면 PARFAIT_NOT_FOUND 예외를 던진다`() {
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns owner
        every { parfaitQueryPort.findByIdAndGroupId(5L, 1L) } returns null

        val exception =
            assertFailsWith<BusinessException> {
                service.updateAll(command(listOf(UpdateParfaitImageItemCommand(201L, 1.0, 1.0, null, null, null))))
            }
        exception.errorCode shouldBe ParfaitImageErrorCode.PARFAIT_NOT_FOUND
    }

    @Test
    fun `이미 마감된 파르페면 PARFAIT_ALREADY_CLOSED 예외를 던진다`() {
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns owner
        every { parfaitQueryPort.findByIdAndGroupId(5L, 1L) } returns activeParfait(status = ParfaitStatus.CLOSED)

        val exception =
            assertFailsWith<BusinessException> {
                service.updateAll(command(listOf(UpdateParfaitImageItemCommand(201L, 1.0, 1.0, null, null, null))))
            }
        exception.errorCode shouldBe ParfaitErrorCode.PARFAIT_ALREADY_CLOSED
    }

    @Test
    fun `그룹 멤버가 아니면 PARFAIT_IMAGE_NOT_OWNED 예외를 던진다`() {
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns null

        val exception =
            assertFailsWith<BusinessException> {
                service.updateAll(command(listOf(UpdateParfaitImageItemCommand(201L, 1.0, 1.0, null, null, null))))
            }
        exception.errorCode shouldBe ParfaitImageErrorCode.PARFAIT_IMAGE_NOT_OWNED
    }

    @Test
    fun `항목 중 하나라도 존재하지 않으면 PARFAIT_IMAGE_NOT_FOUND 예외를 던진다`() {
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns owner
        every { parfaitQueryPort.findByIdAndGroupId(5L, 1L) } returns activeParfait()
        every { parfaitImageQueryPort.findAllByIds(listOf(201L, 999L)) } returns listOf(existingImage(201L))

        val exception =
            assertFailsWith<BusinessException> {
                service.updateAll(
                    command(
                        listOf(
                            UpdateParfaitImageItemCommand(201L, 1.0, 1.0, null, null, null),
                            UpdateParfaitImageItemCommand(999L, 1.0, 1.0, null, null, null),
                        ),
                    ),
                )
            }
        exception.errorCode shouldBe ParfaitImageErrorCode.PARFAIT_IMAGE_NOT_FOUND
    }

    @Test
    fun `항목 중 하나라도 본인이 배치한 토핑이 아니면 PARFAIT_IMAGE_NOT_OWNED 예외를 던진다`() {
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns owner
        every { parfaitQueryPort.findByIdAndGroupId(5L, 1L) } returns activeParfait()
        every { parfaitImageQueryPort.findAllByIds(listOf(201L, 202L)) } returns
            listOf(existingImage(201L), existingImage(202L, placedByGroupMemberId = 999L))

        val exception =
            assertFailsWith<BusinessException> {
                service.updateAll(
                    command(
                        listOf(
                            UpdateParfaitImageItemCommand(201L, 1.0, 1.0, null, null, null),
                            UpdateParfaitImageItemCommand(202L, 1.0, 1.0, null, null, null),
                        ),
                    ),
                )
            }
        exception.errorCode shouldBe ParfaitImageErrorCode.PARFAIT_IMAGE_NOT_OWNED
    }

    @Test
    fun `경로의 parfaitId와 배치의 parfaitId가 다르면 PARFAIT_IMAGE_NOT_FOUND 예외를 던진다`() {
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns owner
        every { parfaitQueryPort.findByIdAndGroupId(5L, 1L) } returns activeParfait()
        every { parfaitImageQueryPort.findAllByIds(listOf(201L)) } returns
            listOf(existingImage(201L, parfaitId = 999L))

        val exception =
            assertFailsWith<BusinessException> {
                service.updateAll(command(listOf(UpdateParfaitImageItemCommand(201L, 1.0, 1.0, null, null, null))))
            }
        exception.errorCode shouldBe ParfaitImageErrorCode.PARFAIT_IMAGE_NOT_FOUND
    }
}
