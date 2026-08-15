package parfait.core.parfait.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import parfait.core.exception.BusinessException
import parfait.core.image.domain.ImageMeta
import parfait.core.image.domain.ImageStatus
import parfait.core.image.domain.ImageType
import parfait.core.image.exception.ImageErrorCode
import parfait.core.image.port.out.ImageMetaQueryPort
import parfait.core.parfait.domain.BackgroundType
import parfait.core.parfait.domain.Parfait
import parfait.core.parfait.domain.ParfaitStatus
import parfait.core.parfait.exception.ParfaitErrorCode
import parfait.core.parfait.port.`in`.ChangeParfaitBackgroundCommand
import parfait.core.parfait.port.out.ParfaitQueryPort
import parfait.core.parfait.port.out.ParfaitSavePort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberQueryPort
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.test.assertFailsWith

class ChangeParfaitBackgroundServiceTest {
    private val parfaitGroupMemberQueryPort = mockk<ParfaitGroupMemberQueryPort>()
    private val parfaitQueryPort = mockk<ParfaitQueryPort>()
    private val parfaitSavePort = mockk<ParfaitSavePort>()
    private val imageMetaQueryPort = mockk<ImageMetaQueryPort>()
    private val service =
        ChangeParfaitBackgroundService(
            parfaitGroupMemberQueryPort,
            parfaitQueryPort,
            parfaitSavePort,
            imageMetaQueryPort,
        )

    private fun parfait(): Parfait =
        Parfait.reconstitute(
            id = 98L,
            parfaitGroupId = 1L,
            parfaitDate = LocalDate.of(2026, 7, 7),
            status = ParfaitStatus.ACTIVE,
            backgroundType = null,
            backgroundValue = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

    private fun command(
        type: BackgroundType = BackgroundType.COLOR,
        value: String? = "#FF5733",
        imageId: Long? = null,
    ) = ChangeParfaitBackgroundCommand(
        memberId = 10L,
        groupId = 1L,
        parfaitId = 98L,
        type = type,
        value = value,
        imageId = imageId,
    )

    private fun confirmedImage(): ImageMeta =
        ImageMeta.reconstitute(
            id = 80L,
            url = "https://s3.example/background/80.png",
            uploadedByMemberId = 10L,
            imageType = ImageType.BACKGROUND,
            status = ImageStatus.COMPLETED,
            referenceCount = 0,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

    @Test
    fun `COLOR 타입이면 value를 그대로 배경으로 저장한다`() {
        every { parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(1L, 10L) } returns true
        every { parfaitQueryPort.findByIdAndGroupId(98L, 1L) } returns parfait()
        every { parfaitSavePort.save(any()) } answers { firstArg() }

        val result = service.change(command(type = BackgroundType.COLOR, value = "#FF5733"))

        result.type shouldBe BackgroundType.COLOR
        result.value shouldBe "#FF5733"
    }

    @Test
    fun `IMAGE 타입이면 확인된 이미지의 URL을 배경으로 저장한다`() {
        every { parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(1L, 10L) } returns true
        every { parfaitQueryPort.findByIdAndGroupId(98L, 1L) } returns parfait()
        every { imageMetaQueryPort.findById(80L) } returns confirmedImage()
        every { parfaitSavePort.save(any()) } answers { firstArg() }

        val result = service.change(command(type = BackgroundType.IMAGE, value = null, imageId = 80L))

        result.type shouldBe BackgroundType.IMAGE
        result.value shouldBe "https://s3.example/background/80.png"
    }

    @Test
    fun `IMAGE 타입인데 존재하지 않는 이미지면 IMAGE_NOT_FOUND를 던진다`() {
        every { parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(1L, 10L) } returns true
        every { parfaitQueryPort.findByIdAndGroupId(98L, 1L) } returns parfait()
        every { imageMetaQueryPort.findById(80L) } returns null

        val exception =
            assertFailsWith<BusinessException> {
                service.change(command(type = BackgroundType.IMAGE, value = null, imageId = 80L))
            }
        exception.errorCode shouldBe ImageErrorCode.IMAGE_NOT_FOUND
    }

    @Test
    fun `IMAGE 타입인데 업로드가 확인되지 않았으면 BACKGROUND_IMAGE_NOT_CONFIRMED를 던진다`() {
        every { parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(1L, 10L) } returns true
        every { parfaitQueryPort.findByIdAndGroupId(98L, 1L) } returns parfait()
        every { imageMetaQueryPort.findById(80L) } returns
            ImageMeta.reconstitute(
                id = 80L,
                url = "https://s3.example/background/80.png",
                uploadedByMemberId = 10L,
                imageType = ImageType.BACKGROUND,
                status = ImageStatus.PENDING,
                referenceCount = 0,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )

        val exception =
            assertFailsWith<BusinessException> {
                service.change(command(type = BackgroundType.IMAGE, value = null, imageId = 80L))
            }
        exception.errorCode shouldBe ParfaitErrorCode.BACKGROUND_IMAGE_NOT_CONFIRMED
    }

    @Test
    fun `COLOR 타입인데 value가 없으면 INVALID_BACKGROUND를 던진다`() {
        every { parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(1L, 10L) } returns true
        every { parfaitQueryPort.findByIdAndGroupId(98L, 1L) } returns parfait()

        val exception =
            assertFailsWith<BusinessException> {
                service.change(command(type = BackgroundType.COLOR, value = null))
            }
        exception.errorCode shouldBe ParfaitErrorCode.INVALID_BACKGROUND
    }

    @Test
    fun `IMAGE 타입인데 imageId가 없으면 INVALID_BACKGROUND를 던진다`() {
        every { parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(1L, 10L) } returns true
        every { parfaitQueryPort.findByIdAndGroupId(98L, 1L) } returns parfait()

        val exception =
            assertFailsWith<BusinessException> {
                service.change(command(type = BackgroundType.IMAGE, value = null, imageId = null))
            }
        exception.errorCode shouldBe ParfaitErrorCode.INVALID_BACKGROUND
    }

    @Test
    fun `존재하지 않거나 다른 그룹 소속인 파르페면 PARFAIT_NOT_FOUND를 던진다`() {
        every { parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(1L, 10L) } returns true
        every { parfaitQueryPort.findByIdAndGroupId(98L, 1L) } returns null

        val exception = assertFailsWith<BusinessException> { service.change(command()) }
        exception.errorCode shouldBe ParfaitErrorCode.PARFAIT_NOT_FOUND
    }

    @Test
    fun `그룹에 참여하지 않은 회원은 변경을 거부한다`() {
        every { parfaitGroupMemberQueryPort.existsByGroupIdAndMemberId(1L, 10L) } returns false

        val exception = assertFailsWith<ParfaitGroupException> { service.change(command()) }
        exception.error shouldBe ParfaitGroupError.GROUP_NOT_JOINED
    }
}
