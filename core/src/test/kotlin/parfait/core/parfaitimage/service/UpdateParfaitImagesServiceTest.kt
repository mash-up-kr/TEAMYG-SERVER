package parfait.core.parfaitimage.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import org.junit.jupiter.api.Test
import parfait.core.exception.BusinessException
import parfait.core.parfait.domain.Parfait
import parfait.core.parfait.domain.ParfaitStatus
import parfait.core.parfait.exception.ParfaitErrorCode
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.core.parfait.service.ParfaitVersionService
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitgroup.domain.ParfaitGroupMember
import parfait.core.parfaitimage.domain.BorderType
import parfait.core.parfaitimage.domain.ParfaitImage
import parfait.core.parfaitimage.exception.ParfaitImageErrorCode
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageItemCommand
import parfait.core.parfaitimage.port.`in`.UpdateParfaitImagesCommand
import parfait.core.parfaitimage.port.`in`.UpdateParfaitTextItemCommand
import parfait.core.parfaitimage.port.out.ParfaitImageQueryPort
import parfait.core.parfaitimage.port.out.ParfaitImageSavePort
import parfait.core.parfaittext.domain.ParfaitText
import parfait.core.parfaittext.exception.ParfaitTextErrorCode
import parfait.core.parfaittext.port.out.ParfaitTextQueryPort
import parfait.core.parfaittext.port.out.ParfaitTextSavePort
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertFailsWith

class UpdateParfaitImagesServiceTest {
    private val parfaitGroupMemberQueryPort = mockk<ParfaitGroupMemberQueryPort>()
    private val parfaitQueryPort = mockk<ParfaitQueryPort>()
    private val parfaitImageQueryPort = mockk<ParfaitImageQueryPort>()
    private val parfaitImageSavePort = mockk<ParfaitImageSavePort>()
    private val parfaitTextQueryPort = mockk<ParfaitTextQueryPort>()
    private val parfaitTextSavePort = mockk<ParfaitTextSavePort>()
    private val parfaitVersionService = mockk<ParfaitVersionService>(relaxed = true)
    private val service =
        UpdateParfaitImagesService(
            parfaitGroupMemberQueryPort = parfaitGroupMemberQueryPort,
            parfaitQueryPort = parfaitQueryPort,
            parfaitImageQueryPort = parfaitImageQueryPort,
            parfaitImageSavePort = parfaitImageSavePort,
            parfaitTextQueryPort = parfaitTextQueryPort,
            parfaitTextSavePort = parfaitTextSavePort,
            parfaitVersionService = parfaitVersionService,
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

    private fun existingText(
        id: Long,
        placedByGroupMemberId: Long = 10L,
        parfaitId: Long = 5L,
    ): ParfaitText =
        ParfaitText.reconstitute(
            id = id,
            parfaitId = parfaitId,
            placedByGroupMemberId = placedByGroupMemberId,
            text = "오늘도 행복한 하루!",
            positionX = 0.0,
            positionY = 0.0,
            positionZ = 0,
            scale = 1.0,
            rotation = 0.0,
            color = "#FF5733",
            fontSize = 16.0,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

    private fun command(
        items: List<UpdateParfaitImageItemCommand>,
        textItems: List<UpdateParfaitTextItemCommand> = emptyList(),
    ) = UpdateParfaitImagesCommand(
        memberId = 42L,
        groupId = 1L,
        parfaitId = 5L,
        items = items,
        textItems = textItems,
    )

    private fun textItem(
        id: Long,
        positionX: Double? = 1.0,
    ) = UpdateParfaitTextItemCommand(id, positionX, null, null, null, null)

    @Test
    fun `여러 토핑을 한 번에 요청하면 API 호출 한 번으로 전부 갱신한다`() {
        every { parfaitImageQueryPort.findAllByIds(listOf(201L, 202L)) } returns
            listOf(existingImage(201L), existingImage(202L))
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns owner
        every { parfaitQueryPort.findByIdAndGroupId(5L, 1L) } returns activeParfait()
        every { parfaitImageSavePort.saveAll(any()) } answers { firstArg() }
        every { parfaitTextQueryPort.findAllByIds(emptyList()) } returns emptyList()
        every { parfaitTextSavePort.saveAll(emptyList()) } returns emptyList()

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

        result.images.size shouldBe 2
        result.images[0].parfaitImageId shouldBe 201L
        result.images[0].positionX shouldBe 200.0
        result.images[0].rotation shouldBe 45.0
        result.images[1].parfaitImageId shouldBe 202L
        result.images[1].positionX shouldBe 10.0
        result.images[1].positionZ shouldBe 2
        result.texts shouldBe emptyList()
    }

    @Test
    fun `항목이 비어있으면 조회·저장 없이 빈 목록을 반환한다`() {
        val result = service.updateAll(command(emptyList()))

        result.images shouldBe emptyList()
        result.texts shouldBe emptyList()
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

    @Test
    fun `일괄 수정에 성공하면 캔버스 version 을 한 번만 올린다`() {
        every { parfaitImageQueryPort.findAllByIds(listOf(201L, 202L)) } returns
            listOf(existingImage(201L), existingImage(202L))
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns owner
        every { parfaitQueryPort.findByIdAndGroupId(5L, 1L) } returns activeParfait()
        every { parfaitImageSavePort.saveAll(any()) } answers { firstArg() }
        every { parfaitTextQueryPort.findAllByIds(emptyList()) } returns emptyList()
        every { parfaitTextSavePort.saveAll(emptyList()) } returns emptyList()

        service.updateAll(
            command(
                listOf(
                    UpdateParfaitImageItemCommand(
                        parfaitImageId = 201L,
                        positionX = 1.0,
                        positionY = 1.0,
                        positionZ = 1,
                        scale = 1.0,
                        rotation = 0.0,
                    ),
                    UpdateParfaitImageItemCommand(
                        parfaitImageId = 202L,
                        positionX = 2.0,
                        positionY = 2.0,
                        positionZ = 2,
                        scale = 1.0,
                        rotation = 0.0,
                    ),
                ),
            ),
        )

        verify(exactly = 1) { parfaitVersionService.bump(5L) }
    }

    @Test
    fun `items 가 비어 있으면 version 을 올리지 않는다`() {
        service.updateAll(command(emptyList()))

        verify(exactly = 0) { parfaitVersionService.bump(any()) }
    }

    @Test
    fun `캔버스 version 은 parfait_image 일괄 저장보다 먼저 올린다 (부모 행 락 먼저)`() {
        every { parfaitImageQueryPort.findAllByIds(listOf(201L)) } returns listOf(existingImage(201L))
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns owner
        every { parfaitQueryPort.findByIdAndGroupId(5L, 1L) } returns activeParfait()
        every { parfaitImageSavePort.saveAll(any()) } answers { firstArg() }
        every { parfaitTextQueryPort.findAllByIds(emptyList()) } returns emptyList()
        every { parfaitTextSavePort.saveAll(emptyList()) } returns emptyList()

        service.updateAll(
            command(
                listOf(
                    UpdateParfaitImageItemCommand(
                        parfaitImageId = 201L,
                        positionX = 1.0,
                        positionY = 1.0,
                        positionZ = 1,
                        scale = 1.0,
                        rotation = 0.0,
                    ),
                ),
            ),
        )

        verifyOrder {
            parfaitVersionService.bump(5L)
            parfaitImageSavePort.saveAll(any())
        }
    }

    @Test
    fun `이미지와 텍스트를 한 번에 수정하고 둘 다 응답한다`() {
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns owner
        every { parfaitQueryPort.findByIdAndGroupId(5L, 1L) } returns activeParfait()
        every { parfaitImageQueryPort.findAllByIds(listOf(201L)) } returns listOf(existingImage(201L))
        every { parfaitTextQueryPort.findAllByIds(listOf(301L)) } returns listOf(existingText(301L))
        every { parfaitImageSavePort.saveAll(any()) } answers { firstArg() }
        every { parfaitTextSavePort.saveAll(any()) } answers { firstArg() }

        val result =
            service.updateAll(
                command(
                    items = listOf(UpdateParfaitImageItemCommand(201L, 200.0, null, null, null, null)),
                    textItems = listOf(UpdateParfaitTextItemCommand(301L, 10.0, 20.0, 3, 2.0, -30.0)),
                ),
            )

        result.images.single().positionX shouldBe 200.0
        val text = result.texts.single()
        text.parfaitTextId shouldBe 301L
        text.text shouldBe "오늘도 행복한 하루!"
        text.positionX shouldBe 10.0
        text.positionY shouldBe 20.0
        text.positionZ shouldBe 3
        text.scale shouldBe 2.0
        text.rotation shouldBe -30.0
        text.color shouldBe "#FF5733"
        verify(exactly = 1) { parfaitVersionService.bump(5L) }
    }

    @Test
    fun `텍스트만 보내도 수정되고 version 을 한 번 올린다`() {
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns owner
        every { parfaitQueryPort.findByIdAndGroupId(5L, 1L) } returns activeParfait()
        every { parfaitImageQueryPort.findAllByIds(emptyList()) } returns emptyList()
        every { parfaitTextQueryPort.findAllByIds(listOf(301L)) } returns listOf(existingText(301L))
        every { parfaitImageSavePort.saveAll(any()) } answers { firstArg() }
        every { parfaitTextSavePort.saveAll(any()) } answers { firstArg() }

        val result = service.updateAll(command(items = emptyList(), textItems = listOf(textItem(301L))))

        result.images shouldBe emptyList()
        result.texts.single().positionX shouldBe 1.0
        verify(exactly = 1) { parfaitVersionService.bump(5L) }
    }

    @Test
    fun `텍스트 항목 중 존재하지 않는 ID가 있으면 PARFAIT_TEXT_NOT_FOUND 예외를 던지고 저장하지 않는다`() {
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns owner
        every { parfaitQueryPort.findByIdAndGroupId(5L, 1L) } returns activeParfait()
        every { parfaitImageQueryPort.findAllByIds(emptyList()) } returns emptyList()
        every { parfaitTextQueryPort.findAllByIds(listOf(999L)) } returns emptyList()

        val exception =
            assertFailsWith<BusinessException> {
                service.updateAll(command(items = emptyList(), textItems = listOf(textItem(999L))))
            }

        exception.errorCode shouldBe ParfaitTextErrorCode.PARFAIT_TEXT_NOT_FOUND
        verify(exactly = 0) { parfaitVersionService.bump(any()) }
    }

    @Test
    fun `경로의 parfaitId와 텍스트의 parfaitId가 다르면 PARFAIT_TEXT_NOT_FOUND 예외를 던진다`() {
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns owner
        every { parfaitQueryPort.findByIdAndGroupId(5L, 1L) } returns activeParfait()
        every { parfaitImageQueryPort.findAllByIds(emptyList()) } returns emptyList()
        every { parfaitTextQueryPort.findAllByIds(listOf(301L)) } returns listOf(existingText(301L, parfaitId = 999L))

        val exception =
            assertFailsWith<BusinessException> {
                service.updateAll(command(items = emptyList(), textItems = listOf(textItem(301L))))
            }

        exception.errorCode shouldBe ParfaitTextErrorCode.PARFAIT_TEXT_NOT_FOUND
    }

    @Test
    fun `본인이 배치하지 않은 텍스트가 섞여 있으면 이미지도 저장하지 않고 PARFAIT_TEXT_NOT_OWNED 예외를 던진다`() {
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns owner
        every { parfaitQueryPort.findByIdAndGroupId(5L, 1L) } returns activeParfait()
        every { parfaitImageQueryPort.findAllByIds(listOf(201L)) } returns listOf(existingImage(201L))
        every { parfaitTextQueryPort.findAllByIds(listOf(301L)) } returns
            listOf(existingText(301L, placedByGroupMemberId = 999L))

        val exception =
            assertFailsWith<BusinessException> {
                service.updateAll(
                    command(
                        items = listOf(UpdateParfaitImageItemCommand(201L, 1.0, 1.0, null, null, null)),
                        textItems = listOf(textItem(301L)),
                    ),
                )
            }

        exception.errorCode shouldBe ParfaitTextErrorCode.PARFAIT_TEXT_NOT_OWNED
        verify(exactly = 0) { parfaitImageSavePort.saveAll(any()) }
        verify(exactly = 0) { parfaitTextSavePort.saveAll(any()) }
        verify(exactly = 0) { parfaitVersionService.bump(any()) }
    }

    @Test
    fun `텍스트 스타일 검증은 수정되지 않는 값으로 통과하고 text 내용은 유지된다`() {
        every { parfaitGroupMemberQueryPort.findByGroupIdAndMemberId(1L, 42L) } returns owner
        every { parfaitQueryPort.findByIdAndGroupId(5L, 1L) } returns activeParfait()
        every { parfaitImageQueryPort.findAllByIds(emptyList()) } returns emptyList()
        every { parfaitTextQueryPort.findAllByIds(listOf(301L)) } returns listOf(existingText(301L))
        every { parfaitImageSavePort.saveAll(any()) } answers { firstArg() }
        every { parfaitTextSavePort.saveAll(any()) } answers { firstArg() }

        val result =
            service.updateAll(
                command(
                    items = emptyList(),
                    textItems = listOf(UpdateParfaitTextItemCommand(301L, null, null, null, null, null)),
                ),
            )

        result.texts.single().text shouldBe "오늘도 행복한 하루!"
    }
}
