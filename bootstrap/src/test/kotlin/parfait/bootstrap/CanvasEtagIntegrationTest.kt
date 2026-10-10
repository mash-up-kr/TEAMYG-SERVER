package parfait.bootstrap

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.StringRedisTemplate
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
import parfait.core.member.port.`in`.WithdrawUseCase
import parfait.core.member.port.out.MemberCreatePort
import parfait.core.parfait.domain.BackgroundType
import parfait.core.parfait.domain.ParfaitDay
import parfait.core.parfait.port.`in`.ChangeParfaitBackgroundCommand
import parfait.core.parfait.port.`in`.ChangeParfaitBackgroundUseCase
import parfait.core.parfait.port.`in`.GetTodayParfaitIfChangedCommand
import parfait.core.parfait.port.`in`.GetTodayParfaitIfChangedResult
import parfait.core.parfait.port.`in`.GetTodayParfaitIfChangedUseCase
import parfait.core.parfaitgroup.application.port.`in`.ChangeMyParfaitGroupNicknameCommand
import parfait.core.parfaitgroup.application.port.`in`.ChangeMyParfaitGroupNicknameUseCase
import parfait.core.parfaitgroup.application.port.`in`.LeaveParfaitGroupCommand
import parfait.core.parfaitgroup.application.port.`in`.LeaveParfaitGroupUseCase
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupMemberSavePort
import parfait.core.parfaitgroup.application.port.out.ParfaitGroupSavePort
import parfait.core.parfaitgroup.domain.InviteCode
import parfait.core.parfaitgroup.domain.NameTagChipType
import parfait.core.parfaitgroup.domain.ParfaitGroup
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException
import parfait.core.parfaitgroup.domain.ParfaitGroupMember
import parfait.core.parfaitimage.domain.BorderType
import parfait.core.parfaitimage.port.`in`.PlaceParfaitImageCommand
import parfait.core.parfaitimage.port.`in`.PlaceParfaitImageUseCase
import kotlin.test.assertFailsWith

/**
 * 실제 MySQL·Redis 컨테이너와 전체 컨텍스트로 "쓰기 -> 같은 ETag로 재요청 -> 200" 흐름을 검증한다.
 * HTTP 대신 v2 유스케이스 빈을 직접 호출한다.
 */
@Testcontainers
@SpringBootTest(
    classes = [ParfaitApplication::class],
    properties = [
        "jwt.secret-key=canvas-etag-test-only-dummy-jwt-secret-key-32-bytes-min",
    ],
)
class CanvasEtagIntegrationTest {
    companion object {
        private const val STALE_PARFAIT_ID = 9_999_999L

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

    @Autowired private lateinit var getTodayV2: GetTodayParfaitIfChangedUseCase

    @Autowired private lateinit var changeBackground: ChangeParfaitBackgroundUseCase

    @Autowired private lateinit var changeNickname: ChangeMyParfaitGroupNicknameUseCase

    @Autowired private lateinit var leaveGroup: LeaveParfaitGroupUseCase

    @Autowired private lateinit var memberCreatePort: MemberCreatePort

    @Autowired private lateinit var groupSavePort: ParfaitGroupSavePort

    @Autowired private lateinit var groupMemberSavePort: ParfaitGroupMemberSavePort

    @Autowired private lateinit var withdraw: WithdrawUseCase

    @Autowired private lateinit var placeImage: PlaceParfaitImageUseCase

    @Autowired private lateinit var imageMetaSavePort: ImageMetaSavePort

    // Redis 오류를 삼키는 구조라, 캐시가 죽었거나 NoOp 이어도 ETag 단언은 통과한다. 키를 직접 확인해 캐시가 실제로 쓰이는지 본다.
    @Autowired private lateinit var redisTemplate: StringRedisTemplate

    private fun cached(key: String): Boolean = redisTemplate.hasKey(key)

    private fun parfaitVersionKey(parfaitId: Long) = "parfait:version:$parfaitId"

    private fun groupVersionKey(groupId: Long) = "group:version:$groupId"

    private fun memberKey(
        groupId: Long,
        memberId: Long,
    ) = "group:member:$groupId:$memberId"

    private fun todayKey(groupId: Long) = "parfait:today:$groupId:${ParfaitDay.current()}"

    private data class Fixture(
        val groupId: Long,
        val memberId: Long,
        val otherMemberId: Long,
    )

    private fun fixture(inviteCode: String): Fixture {
        val memberId = memberCreatePort.create(LoginProvider.KAKAO, "$inviteCode-a", "멤버A")
        val otherMemberId = memberCreatePort.create(LoginProvider.KAKAO, "$inviteCode-b", "멤버B")
        val groupId =
            groupSavePort
                .save(ParfaitGroup.create(name = "ETag그룹", inviteCode = InviteCode.of(inviteCode), memberLimit = 12))
                .requireId()
        groupMemberSavePort.save(
            ParfaitGroupMember.join(
                parfaitGroupId = groupId,
                memberId = memberId,
                groupNickname = "닉A",
                nametagChip = NameTagChipType.TYPE1,
            ),
        )
        groupMemberSavePort.save(
            ParfaitGroupMember.join(
                parfaitGroupId = groupId,
                memberId = otherMemberId,
                groupNickname = "닉B",
                nametagChip = NameTagChipType.TYPE2,
            ),
        )
        return Fixture(groupId, memberId, otherMemberId)
    }

    private fun poll(
        f: Fixture,
        memberId: Long,
        ifNoneMatch: String?,
    ): GetTodayParfaitIfChangedResult =
        getTodayV2.get(
            GetTodayParfaitIfChangedCommand(memberId = memberId, groupId = f.groupId, ifNoneMatch = ifNoneMatch),
        )

    @Test
    fun `변경이 없으면 304 이고 배경을 바꾸면 같은 ETag 로 재요청해도 200 이 나온다`() {
        val f = fixture("ETG001")

        val first = poll(f, f.memberId, null) as GetTodayParfaitIfChangedResult.Modified
        val otherFirst = poll(f, f.otherMemberId, null) as GetTodayParfaitIfChangedResult.Modified
        poll(f, f.memberId, first.etag) shouldBe GetTodayParfaitIfChangedResult.NotModified(first.etag)
        poll(f, f.otherMemberId, otherFirst.etag) shouldBe GetTodayParfaitIfChangedResult.NotModified(otherFirst.etag)

        val parfaitId = first.body.parfaitId
        cached(parfaitVersionKey(parfaitId)) shouldBe true
        cached(groupVersionKey(f.groupId)) shouldBe true
        cached(memberKey(f.groupId, f.memberId)) shouldBe true
        cached(memberKey(f.groupId, f.otherMemberId)) shouldBe true
        cached(todayKey(f.groupId)) shouldBe true

        changeBackground.change(
            ChangeParfaitBackgroundCommand(
                memberId = f.memberId,
                groupId = f.groupId,
                parfaitId = first.body.parfaitId,
                type = BackgroundType.COLOR,
                value = "#FF5733",
                imageId = null,
            ),
        )

        // 커밋 후 리스너가 캔버스 version 키만 지운다.
        cached(parfaitVersionKey(parfaitId)) shouldBe false
        cached(groupVersionKey(f.groupId)) shouldBe true

        // 변경한 본인과 다른 멤버 모두, 변경 전에 받은 ETag 로 재요청하면 200 과 새 ETag 를 받는다.
        val mine = poll(f, f.memberId, first.etag)
        (mine is GetTodayParfaitIfChangedResult.Modified) shouldBe true
        mine.etag shouldNotBe first.etag
        val theirs = poll(f, f.otherMemberId, otherFirst.etag)
        (theirs is GetTodayParfaitIfChangedResult.Modified) shouldBe true
        theirs.etag shouldNotBe otherFirst.etag

        // 새 ETag 로 다시 요청하면 304 로 돌아온다.
        poll(f, f.memberId, mine.etag) shouldBe GetTodayParfaitIfChangedResult.NotModified(mine.etag)
    }

    @Test
    fun `닉네임을 바꾸면 같은 ETag 로 재요청해도 200 이 나온다`() {
        val f = fixture("ETG002")
        val first = poll(f, f.memberId, null) as GetTodayParfaitIfChangedResult.Modified
        poll(f, f.memberId, first.etag) shouldBe GetTodayParfaitIfChangedResult.NotModified(first.etag)
        cached(groupVersionKey(f.groupId)) shouldBe true

        changeNickname.change(
            ChangeMyParfaitGroupNicknameCommand(memberId = f.otherMemberId, groupId = f.groupId, groupNickname = "새닉B"),
        )

        cached(groupVersionKey(f.groupId)) shouldBe false

        val after = poll(f, f.memberId, first.etag)
        (after is GetTodayParfaitIfChangedResult.Modified) shouldBe true
        after.etag shouldNotBe first.etag
    }

    @Test
    fun `그룹을 나간 멤버는 캐시가 있어도 바로 거부된다`() {
        val f = fixture("ETG003")
        val first = poll(f, f.otherMemberId, null) as GetTodayParfaitIfChangedResult.Modified
        poll(f, f.otherMemberId, first.etag) shouldBe GetTodayParfaitIfChangedResult.NotModified(first.etag)
        cached(memberKey(f.groupId, f.otherMemberId)) shouldBe true

        leaveGroup.leave(LeaveParfaitGroupCommand(memberId = f.otherMemberId, groupId = f.groupId))

        cached(memberKey(f.groupId, f.otherMemberId)) shouldBe false

        assertFailsWith<ParfaitGroupException> { poll(f, f.otherMemberId, first.etag) }
            .error shouldBe ParfaitGroupError.GROUP_NOT_JOINED
    }

    @Test
    fun `멤버가 아닌 사용자는 ETag 없이 거부된다`() {
        val f = fixture("ETG004")
        val stranger = memberCreatePort.create(LoginProvider.KAKAO, "ETG004-x", "외부인")

        assertFailsWith<ParfaitGroupException> { poll(f, stranger, null) }
            .error shouldBe ParfaitGroupError.GROUP_NOT_JOINED
    }

    @Test
    fun `한 멤버가 그룹을 나가면 남은 멤버는 변경 전 ETag 로 재요청해도 200 과 새 ETag 를 받는다`() {
        val f = fixture("ETG005")
        val first = poll(f, f.memberId, null) as GetTodayParfaitIfChangedResult.Modified
        poll(f, f.memberId, first.etag) shouldBe GetTodayParfaitIfChangedResult.NotModified(first.etag)

        leaveGroup.leave(LeaveParfaitGroupCommand(memberId = f.otherMemberId, groupId = f.groupId))

        val after = poll(f, f.memberId, first.etag)
        (after is GetTodayParfaitIfChangedResult.Modified) shouldBe true
        after.etag shouldNotBe first.etag
    }

    @Test
    fun `한 멤버가 회원 탈퇴하면 남은 멤버는 변경 전 ETag 로 재요청해도 200 을 받는다`() {
        val f = fixture("ETG006")
        val first = poll(f, f.memberId, null) as GetTodayParfaitIfChangedResult.Modified
        poll(f, f.otherMemberId, null)
        poll(f, f.memberId, first.etag) shouldBe GetTodayParfaitIfChangedResult.NotModified(first.etag)
        cached(memberKey(f.groupId, f.otherMemberId)) shouldBe true

        withdraw.withdraw(f.otherMemberId)

        cached(groupVersionKey(f.groupId)) shouldBe false
        cached(memberKey(f.groupId, f.otherMemberId)) shouldBe false
        val after = poll(f, f.memberId, first.etag)
        (after is GetTodayParfaitIfChangedResult.Modified) shouldBe true
        after.etag shouldNotBe first.etag
    }

    @Test
    fun `새 이미지를 배치하면 같은 ETag 로 재요청해도 200 이 나온다`() {
        val f = fixture("ETG007")
        val first = poll(f, f.memberId, null) as GetTodayParfaitIfChangedResult.Modified
        poll(f, f.memberId, first.etag) shouldBe GetTodayParfaitIfChangedResult.NotModified(first.etag)
        val parfaitId = first.body.parfaitId
        cached(parfaitVersionKey(parfaitId)) shouldBe true

        val pending =
            imageMetaSavePort.save(
                ImageMeta.createPending(
                    url = "https://example.com/etag/ETG007.png",
                    uploadedByMemberId = f.otherMemberId,
                    imageType = ImageType.NUKKI,
                ),
            )
        val imageId = imageMetaSavePort.save(pending.confirm()).requireId()
        placeImage.place(
            PlaceParfaitImageCommand(
                memberId = f.otherMemberId,
                groupId = f.groupId,
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
            ),
        )

        cached(parfaitVersionKey(parfaitId)) shouldBe false
        val after = poll(f, f.memberId, first.etag)
        (after is GetTodayParfaitIfChangedResult.Modified) shouldBe true
        after.etag shouldNotBe first.etag
    }

    @Test
    fun `오늘 캔버스 id 캐시가 DB 에 없는 id 를 가리키면 캐시를 복구하고 200 을 돌려준다`() {
        val f = fixture("ETG008")
        val first = poll(f, f.memberId, null) as GetTodayParfaitIfChangedResult.Modified
        val todayParfaitId = first.body.parfaitId
        redisTemplate.opsForValue().get(todayKey(f.groupId)) shouldBe todayParfaitId.toString()

        // DB 를 복원·초기화했는데 Redis 는 그대로인 상황: 캐시가 DB 에 없는 id 를 가리킨다.
        redisTemplate.opsForValue().set(todayKey(f.groupId), STALE_PARFAIT_ID.toString())

        val after = poll(f, f.memberId, null)

        (after is GetTodayParfaitIfChangedResult.Modified) shouldBe true
        after as GetTodayParfaitIfChangedResult.Modified
        after.body.parfaitId shouldBe todayParfaitId
        after.etag shouldBe first.etag
        redisTemplate.opsForValue().get(todayKey(f.groupId)) shouldBe todayParfaitId.toString()
    }
}
