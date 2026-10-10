package parfait.bootstrap

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName
import parfait.core.auth.domain.LoginProvider
import parfait.core.image.domain.ImageMeta
import parfait.core.image.domain.ImageType
import parfait.core.image.port.out.ImageMetaSavePort
import parfait.core.member.port.out.MemberCreatePort
import parfait.core.parfait.domain.ParfaitDay
import parfait.core.parfait.port.`in`.EnsureActiveCanvasUseCase
import parfait.core.parfait.port.out.ParfaitVersionPort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberSavePort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupSavePort
import parfait.core.parfaitgroup.domain.InviteCode
import parfait.core.parfaitgroup.domain.NameTagChipType
import parfait.core.parfaitgroup.domain.ParfaitGroup
import parfait.core.parfaitgroup.domain.ParfaitGroupMember
import parfait.core.parfaitimage.domain.BorderType
import parfait.core.parfaitimage.port.`in`.PlaceParfaitImageCommand
import parfait.core.parfaitimage.port.`in`.PlaceParfaitImageUseCase
import java.util.Collections
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * 같은 캔버스에 두 멤버가 동시에 "새" 이미지를 배치할 때 데드락(MySQL 1213)이 나지 않는지 검증한다.
 *
 * parfait_image INSERT 는 FK(fk_parfait_image_parfait) 검사로 부모 parfait 행에 공유 락(S)을 건다.
 * 그 뒤에 같은 행을 UPDATE(version 증가)하면 S -> X 승격이 필요해, 두 트랜잭션이 서로의 S 락을 기다리며 데드락이 날 수 있다.
 */
@Testcontainers
@SpringBootTest(
    classes = [ParfaitApplication::class],
    properties = [
        "jwt.secret-key=concurrent-place-test-only-dummy-jwt-secret-key-32-bytes",
    ],
)
class ConcurrentPlaceDeadlockTest {
    companion object {
        private const val ROUNDS = 30

        @Container
        @JvmStatic
        val mysql = MySQLContainer("mysql:8.4")

        @Container
        @JvmStatic
        val redis: GenericContainer<*> =
            GenericContainer(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379)

        @DynamicPropertySource
        @JvmStatic
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { mysql.jdbcUrl }
            registry.add("spring.datasource.username") { mysql.username }
            registry.add("spring.datasource.password") { mysql.password }
            registry.add("spring.data.redis.host") { redis.host }
            registry.add("spring.data.redis.port") { redis.getMappedPort(6379) }
        }
    }

    @Autowired private lateinit var placeImage: PlaceParfaitImageUseCase

    @Autowired private lateinit var ensureActiveCanvas: EnsureActiveCanvasUseCase

    @Autowired private lateinit var parfaitVersionPort: ParfaitVersionPort

    @Autowired private lateinit var imageMetaSavePort: ImageMetaSavePort

    @Autowired private lateinit var memberCreatePort: MemberCreatePort

    @Autowired private lateinit var groupSavePort: ParfaitGroupSavePort

    @Autowired private lateinit var groupMemberSavePort: ParfaitGroupMemberSavePort

    private fun completedImage(
        uploaderMemberId: Long,
        key: String,
    ): Long {
        val pending =
            imageMetaSavePort.save(
                ImageMeta.createPending(
                    url = "https://example.com/deadlock/$key.png",
                    uploadedByMemberId = uploaderMemberId,
                    imageType = ImageType.NUKKI,
                ),
            )
        return imageMetaSavePort.save(pending.confirm()).requireId()
    }

    private fun command(
        memberId: Long,
        groupId: Long,
        parfaitId: Long,
        imageId: Long,
    ) = PlaceParfaitImageCommand(
        memberId = memberId,
        groupId = groupId,
        parfaitId = parfaitId,
        imageId = imageId,
        positionX = 0.5,
        positionY = 0.5,
        positionZ = 1,
        scale = 1.0,
        rotation = 0.0,
        borderType = BorderType.NONE,
        borderColor = null,
        borderWidth = null,
    )

    @Test
    fun `같은 캔버스에 두 멤버가 동시에 새 이미지를 배치해도 데드락 없이 모두 성공한다`() {
        val memberA = memberCreatePort.create(LoginProvider.KAKAO, "DLK001-a", "멤버A")
        val memberB = memberCreatePort.create(LoginProvider.KAKAO, "DLK001-b", "멤버B")
        val groupId =
            groupSavePort
                .save(ParfaitGroup.create(name = "데드락그룹", inviteCode = InviteCode.of("DLK001"), memberLimit = 12))
                .requireId()
        groupMemberSavePort.save(
            ParfaitGroupMember.join(
                parfaitGroupId = groupId,
                memberId = memberA,
                groupNickname = "닉A",
                nametagChip = NameTagChipType.TYPE1,
            ),
        )
        groupMemberSavePort.save(
            ParfaitGroupMember.join(
                parfaitGroupId = groupId,
                memberId = memberB,
                groupNickname = "닉B",
                nametagChip = NameTagChipType.TYPE2,
            ),
        )
        val parfaitId = ensureActiveCanvas.ensure(groupId, ParfaitDay.current()).requireId()
        val initialVersion = parfaitVersionPort.getVersion(parfaitId)

        val failures = Collections.synchronizedList(mutableListOf<Throwable>())
        var successes = 0
        val executor = Executors.newFixedThreadPool(2)
        try {
            repeat(ROUNDS) { round ->
                val imageA = completedImage(memberA, "r$round-a")
                val imageB = completedImage(memberB, "r$round-b")
                val barrier = CyclicBarrier(2)
                val futures =
                    listOf(memberA to imageA, memberB to imageB).map { (memberId, imageId) ->
                        executor.submit<Boolean> {
                            barrier.await(10, TimeUnit.SECONDS)
                            try {
                                placeImage.place(command(memberId, groupId, parfaitId, imageId))
                                true
                            } catch (e: Throwable) {
                                failures.add(e)
                                false
                            }
                        }
                    }
                successes += futures.count { it.get(30, TimeUnit.SECONDS) }
            }
        } finally {
            executor.shutdownNow()
        }

        if (failures.isNotEmpty()) {
            val summary =
                failures
                    .groupingBy { "${it::class.qualifiedName}: ${rootCauseMessage(it)}" }
                    .eachCount()
                    .entries
                    .joinToString("\n") { (k, v) -> "  [${v}회] $k" }
            throw AssertionError(
                "동시 배치 ${ROUNDS * 2}건 중 ${failures.size}건 실패:\n$summary",
                failures.first(),
            )
        }
        successes shouldBe ROUNDS * 2
        parfaitVersionPort.getVersion(parfaitId) shouldBe initialVersion + successes
    }

    private fun rootCauseMessage(t: Throwable): String {
        var cur = t
        while (cur.cause != null && cur.cause !== cur) cur = cur.cause!!
        return "${cur::class.simpleName}: ${cur.message}"
    }
}
