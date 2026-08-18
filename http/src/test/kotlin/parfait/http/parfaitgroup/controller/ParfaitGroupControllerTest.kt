package parfait.http.parfaitgroup.controller

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import parfait.core.parfaitgroup.application.port.`in`.ChangeMyParfaitGroupNicknameResult
import parfait.core.parfaitgroup.application.port.`in`.ChangeMyParfaitGroupNicknameUseCase
import parfait.core.parfaitgroup.application.port.`in`.CreateParfaitGroupResult
import parfait.core.parfaitgroup.application.port.`in`.CreateParfaitGroupUseCase
import parfait.core.parfaitgroup.application.port.`in`.GetMyParfaitGroupDetailUseCase
import parfait.core.parfaitgroup.application.port.`in`.GetMyParfaitGroupsUseCase
import parfait.core.parfaitgroup.application.port.`in`.JoinParfaitGroupResult
import parfait.core.parfaitgroup.application.port.`in`.JoinParfaitGroupUseCase
import parfait.core.parfaitgroup.application.port.`in`.LeaveParfaitGroupResult
import parfait.core.parfaitgroup.application.port.`in`.LeaveParfaitGroupUseCase
import parfait.core.parfaitgroup.application.port.`in`.MyParfaitGroupDetailResult
import parfait.core.parfaitgroup.application.port.`in`.MyParfaitGroupResult
import parfait.core.parfaitgroup.application.port.`in`.ParfaitGroupMemberResult
import parfait.core.parfaitgroup.application.port.`in`.PreviewParfaitGroupJoinResult
import parfait.core.parfaitgroup.application.port.`in`.PreviewParfaitGroupJoinUseCase
import parfait.core.parfaitgroup.application.port.`in`.ReportParfaitGroupResult
import parfait.core.parfaitgroup.application.port.`in`.ReportParfaitGroupUseCase
import parfait.core.parfaitgroup.domain.NameTagChipType
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig
import java.time.LocalDateTime

@WebMvcTest(controllers = [ParfaitGroupController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalExceptionHandler::class,
    ParfaitGroupControllerTest.UseCaseConfig::class,
    TestMemberQueryPortConfig::class,
    TestTokenValidatePortConfig::class,
)
class ParfaitGroupControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var previewUseCase: PreviewParfaitGroupJoinUseCase

    @Autowired
    private lateinit var joinUseCase: JoinParfaitGroupUseCase

    @Autowired
    private lateinit var createUseCase: CreateParfaitGroupUseCase

    @Autowired
    private lateinit var getMyGroupsUseCase: GetMyParfaitGroupsUseCase

    @Autowired
    private lateinit var getMyGroupDetailUseCase: GetMyParfaitGroupDetailUseCase

    @Autowired
    private lateinit var changeNicknameUseCase: ChangeMyParfaitGroupNicknameUseCase

    @Autowired
    private lateinit var leaveUseCase: LeaveParfaitGroupUseCase

    @Autowired
    private lateinit var reportUseCase: ReportParfaitGroupUseCase

    private val authentication = UsernamePasswordAuthenticationToken("42", null, emptyList())

    @Test
    fun `참여 미리보기는 인증 회원과 초대코드를 검증하고 그룹명을 응답한다`() {
        every { previewUseCase.preview(42L, "ABCD12") } returns
            PreviewParfaitGroupJoinResult(groupName = "우리 그룹")

        mockMvc
            .get("/api/parfait-groups/join-preview") {
                principal = authentication
                param("inviteCode", "ABCD12")
            }.andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data.groupName") { value("우리 그룹") }
            }

        verify { previewUseCase.preview(42L, "ABCD12") }
    }

    @Test
    fun `내 그룹 목록은 최근 이미지 정보와 마지막 토퍼의 Nametag-Chip을 함께 반환한다`() {
        every { getMyGroupsUseCase.getAll(42L) } returns
            listOf(
                MyParfaitGroupResult(
                    groupId = 1L,
                    groupName = "우리 그룹",
                    recentImageUrl = "https://image.example/latest",
                    recentImageUploadedAt = LocalDateTime.of(2026, 8, 1, 12, 0),
                    lastPlacedByNametagChip = NameTagChipType.TYPE7,
                ),
            )

        mockMvc
            .get("/api/parfait-groups") {
                principal = authentication
            }.andExpect {
                status { isOk() }
                jsonPath("$.data[0].groupId") { value(1) }
                jsonPath("$.data[0].groupName") { value("우리 그룹") }
                jsonPath("$.data[0].recentImageUrl") { value("https://image.example/latest") }
                jsonPath("$.data[0].recentImageUploadedAt") { value("2026-08-01T12:00:00") }
                jsonPath("$.data[0].lastPlacedByNametagChip") { value("TYPE7") }
            }
    }

    @Test
    fun `최근 이미지가 없는 그룹은 recentImageUrl만 null이고 나머지는 생성자 정보로 채워진다`() {
        every { getMyGroupsUseCase.getAll(42L) } returns
            listOf(
                MyParfaitGroupResult(
                    groupId = 1L,
                    groupName = "이미지 없는 그룹",
                    recentImageUrl = null,
                    recentImageUploadedAt = LocalDateTime.of(2026, 8, 1, 0, 0),
                    lastPlacedByNametagChip = NameTagChipType.TYPE3,
                ),
            )

        mockMvc
            .get("/api/parfait-groups") {
                principal = authentication
            }.andExpect {
                status { isOk() }
                jsonPath("$.data[0].recentImageUrl") { value(nullValue()) }
                jsonPath("$.data[0].recentImageUploadedAt") { value("2026-08-01T00:00:00") }
                jsonPath("$.data[0].lastPlacedByNametagChip") { value("TYPE3") }
            }
    }

    @Test
    fun `내 그룹 상세는 그룹명 닉네임 그룹원 초대코드 정원과 멤버별 Nametag-Chip을 반환한다`() {
        every { getMyGroupDetailUseCase.get(42L, 1L) } returns
            MyParfaitGroupDetailResult(
                groupId = 1L,
                groupName = "우리 그룹",
                groupNickname = "내 닉네임",
                inviteCode = "ABCD12",
                memberLimit = 12,
                members = listOf(ParfaitGroupMemberResult(42L, "내 닉네임", NameTagChipType.TYPE3)),
            )

        mockMvc
            .get("/api/parfait-groups/1") {
                principal = authentication
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.groupId") { value(1) }
                jsonPath("$.data.groupName") { value("우리 그룹") }
                jsonPath("$.data.groupNickname") { value("내 닉네임") }
                jsonPath("$.data.inviteCode") { value("ABCD12") }
                jsonPath("$.data.memberLimit") { value(12) }
                jsonPath("$.data.members[0].memberId") { value(42) }
                jsonPath("$.data.members[0].nametagChip") { value("TYPE3") }
            }
    }

    @Test
    fun `참여 미리보기 실패는 UX 분기용 그룹 오류 코드로 응답한다`() {
        every { previewUseCase.preview(42L, "ABCD12") } throws
            ParfaitGroupException(ParfaitGroupError.GROUP_MEMBER_LIMIT_REACHED)

        mockMvc
            .get("/api/parfait-groups/join-preview") {
                principal = authentication
                param("inviteCode", "ABCD12")
            }.andExpect {
                status { isConflict() }
                jsonPath("$.success") { value(false) }
                jsonPath("$.code") { value("GROUP_MEMBER_LIMIT_REACHED") }
            }
    }

    @Test
    fun `그룹 참여는 SecurityContext의 회원 id를 command에 담는다`() {
        every { joinUseCase.join(any()) } returns JoinParfaitGroupResult(1L, "우리 그룹")

        mockMvc
            .post("/api/parfait-groups/join") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"inviteCode":"ABCD12"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.groupId") { value(1) }
                jsonPath("$.data.groupName") { value("우리 그룹") }
            }

        verify {
            joinUseCase.join(
                match { it.memberId == 42L && it.inviteCode == "ABCD12" },
            )
        }
    }

    @Test
    fun `그룹 생성은 생성된 그룹 정보와 초대코드를 201로 응답한다`() {
        every { createUseCase.create(any()) } returns
            CreateParfaitGroupResult(
                groupId = 1L,
                groupName = "우리 그룹",
                inviteCode = "ABCD12",
                memberLimit = 12,
                recentImageUrl = null,
                recentImageUploadedAt = LocalDateTime.of(2026, 8, 1, 0, 0),
                lastPlacedByNametagChip = NameTagChipType.TYPE1,
            )

        mockMvc
            .post("/api/parfait-groups") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content =
                    """{"groupName":"우리 그룹","groupNickname":"내 닉네임","memberLimit":12}"""
            }.andExpect {
                status { isCreated() }
                jsonPath("$.code") { value("CREATED") }
                jsonPath("$.data.groupId") { value(1) }
                jsonPath("$.data.inviteCode") { value("ABCD12") }
                jsonPath("$.data.memberLimit") { value(12) }
            }

        verify {
            createUseCase.create(
                match {
                    it.memberId == 42L &&
                        it.groupName == "우리 그룹" &&
                        it.groupNickname == "내 닉네임" &&
                        it.memberLimit == 12
                },
            )
        }
    }

    @Test
    fun `그룹 닉네임 변경은 인증 회원과 경로 그룹 id를 command에 담는다`() {
        every { changeNicknameUseCase.change(any()) } returns ChangeMyParfaitGroupNicknameResult(1L, "새 닉네임")

        mockMvc
            .patch("/api/parfait-groups/1/nickname") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"groupNickname":"새 닉네임"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.groupNickname") { value("새 닉네임") }
            }

        verify {
            changeNicknameUseCase.change(
                match { it.memberId == 42L && it.groupId == 1L && it.groupNickname == "새 닉네임" },
            )
        }
    }

    @Test
    fun `그룹 나가기는 인증 회원과 경로 그룹 id를 command에 담는다`() {
        every { leaveUseCase.leave(any()) } returns LeaveParfaitGroupResult(1L)

        mockMvc
            .delete("/api/parfait-groups/1/members/me") {
                principal = authentication
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.groupId") { value(1) }
            }

        verify { leaveUseCase.leave(match { it.memberId == 42L && it.groupId == 1L }) }
    }

    @Test
    fun `그룹 신고는 신고를 생성하고 같은 요청에서 탈퇴 결과를 반환한다`() {
        every { reportUseCase.report(any()) } returns ReportParfaitGroupResult(1L, 99L)

        mockMvc
            .post("/api/parfait-groups/1/reports") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"reason":"부적절한 그룹입니다"}"""
            }.andExpect {
                status { isCreated() }
                jsonPath("$.code") { value("CREATED") }
                jsonPath("$.data.groupId") { value(1) }
                jsonPath("$.data.reportId") { value(99) }
            }

        verify {
            reportUseCase.report(
                match { it.memberId == 42L && it.groupId == 1L && it.reason == "부적절한 그룹입니다" },
            )
        }
    }

    @TestConfiguration
    class UseCaseConfig {
        @Bean
        fun previewUseCase(): PreviewParfaitGroupJoinUseCase = mockk()

        @Bean
        fun joinUseCase(): JoinParfaitGroupUseCase = mockk()

        @Bean
        fun createUseCase(): CreateParfaitGroupUseCase = mockk()

        @Bean
        fun getMyParfaitGroupsUseCase(): GetMyParfaitGroupsUseCase = mockk()

        @Bean
        fun getMyParfaitGroupDetailUseCase(): GetMyParfaitGroupDetailUseCase = mockk()

        @Bean
        fun changeMyParfaitGroupNicknameUseCase(): ChangeMyParfaitGroupNicknameUseCase = mockk()

        @Bean
        fun leaveParfaitGroupUseCase(): LeaveParfaitGroupUseCase = mockk()

        @Bean
        fun reportParfaitGroupUseCase(): ReportParfaitGroupUseCase = mockk()
    }
}
