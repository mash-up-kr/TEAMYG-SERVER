package parfait.http.parfait.controller

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import parfait.core.exception.BusinessException
import parfait.core.parfait.domain.BackgroundType
import parfait.core.parfait.domain.ParfaitStatus
import parfait.core.parfait.exception.ParfaitErrorCode
import parfait.core.parfait.port.`in`.BackgroundResult
import parfait.core.parfait.port.`in`.GetParfaitDetailUseCase
import parfait.core.parfait.port.`in`.GetParfaitYearsUseCase
import parfait.core.parfait.port.`in`.GetPastParfaitsUseCase
import parfait.core.parfait.port.`in`.GetTodayParfaitResult
import parfait.core.parfait.port.`in`.GetTodayParfaitUseCase
import parfait.core.parfait.port.`in`.GroupMemberResult
import parfait.core.parfait.port.`in`.PastParfaitResult
import parfait.core.parfait.port.`in`.PlacedByResult
import parfait.core.parfait.port.`in`.TodayParfaitImageResult
import parfait.core.parfaitgroup.domain.NameTagChipType
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException
import parfait.core.parfaitimage.domain.BorderType
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(controllers = [ParfaitController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalExceptionHandler::class,
    ParfaitControllerTest.UseCaseConfig::class,
    TestMemberQueryPortConfig::class,
    TestTokenValidatePortConfig::class,
)
class ParfaitControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var getParfaitYearsUseCase: GetParfaitYearsUseCase

    @Autowired
    private lateinit var getPastParfaitsUseCase: GetPastParfaitsUseCase

    @Autowired
    private lateinit var getTodayParfaitUseCase: GetTodayParfaitUseCase

    @Autowired
    private lateinit var getParfaitDetailUseCase: GetParfaitDetailUseCase

    private val authentication = UsernamePasswordAuthenticationToken("42", null, emptyList())

    @Test
    fun `범위를 지정해 조회하면 200과 파르페 목록을 응답한다`() {
        every { getPastParfaitsUseCase.getPastParfaits(any()) } returns
            listOf(
                PastParfaitResult(
                    parfaitId = 98L,
                    date = LocalDate.of(2026, 7, 7),
                    thumbnailUrl = "https://parfait-bucket.s3.../completed/group1/2026-07-07.png",
                    imageCount = 4,
                ),
            )

        mockMvc
            .get("/api/v1/groups/1/parfaits") {
                principal = authentication
                param("from", "2026-07-01")
                param("to", "2026-07-31")
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.parfaits[0].parfaitId") { value(98) }
                jsonPath("$.data.parfaits[0].date") { value("2026-07-07") }
                jsonPath("$.data.parfaits[0].imageCount") { value(4) }
            }

        verify {
            getPastParfaitsUseCase.getPastParfaits(
                match {
                    it.memberId == 42L &&
                        it.groupId == 1L &&
                        it.from == LocalDate.of(2026, 7, 1) &&
                        it.to == LocalDate.of(2026, 7, 31)
                },
            )
        }
    }

    @Test
    fun `from과 to를 생략하면 null로 UseCase에 전달한다`() {
        every { getPastParfaitsUseCase.getPastParfaits(any()) } returns emptyList()

        mockMvc
            .get("/api/v1/groups/1/parfaits") {
                principal = authentication
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.parfaits") { isEmpty() }
            }

        verify {
            getPastParfaitsUseCase.getPastParfaits(
                match { it.from == null && it.to == null },
            )
        }
    }

    @Test
    fun `조회 시작일이 종료일보다 늦으면 400과 INVALID_DATE_RANGE로 응답한다`() {
        every { getPastParfaitsUseCase.getPastParfaits(any()) } throws
            BusinessException(ParfaitErrorCode.INVALID_DATE_RANGE)

        mockMvc
            .get("/api/v1/groups/1/parfaits") {
                principal = authentication
                param("from", "2026-08-01")
                param("to", "2026-07-01")
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("INVALID_DATE_RANGE") }
            }
    }

    @Test
    fun `그룹에 참여하지 않았으면 403과 GROUP_NOT_JOINED로 응답한다 (과거 목록 조회)`() {
        every { getPastParfaitsUseCase.getPastParfaits(any()) } throws
            ParfaitGroupException(ParfaitGroupError.GROUP_NOT_JOINED)

        mockMvc
            .get("/api/v1/groups/1/parfaits") {
                principal = authentication
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.code") { value("GROUP_NOT_JOINED") }
            }
    }

    @Test
    fun `오늘의 파르페 조회에 성공하면 200과 캔버스 정보를 응답한다`() {
        every { getTodayParfaitUseCase.get(any()) } returns
            GetTodayParfaitResult(
                parfaitId = 100L,
                date = LocalDate.of(2026, 7, 9),
                status = ParfaitStatus.ACTIVE,
                lastClosedDate = LocalDate.of(2026, 7, 8),
                groupMembers =
                    listOf(
                        GroupMemberResult(id = 10L, nickname = "연경이", nametagChip = NameTagChipType.TYPE1),
                        GroupMemberResult(id = 11L, nickname = "서휘", nametagChip = NameTagChipType.TYPE2),
                    ),
                background = BackgroundResult(type = BackgroundType.COLOR, value = "#FFFFFF"),
                images =
                    listOf(
                        TodayParfaitImageResult(
                            parfaitImageId = 201L,
                            imageId = 77L,
                            imageUrl = "https://parfait-bucket.s3.../nukki/user1/550e8400.png",
                            positionX = 120.5,
                            positionY = 340.2,
                            positionZ = 1,
                            scale = 1.0,
                            rotation = 0.0,
                            borderType = BorderType.SOLID,
                            borderColor = "#000000",
                            borderWidth = 2.0,
                            placedBy =
                                PlacedByResult(
                                    groupMemberId = 10L,
                                    nickname = "연경이",
                                    nametagChip = NameTagChipType.TYPE6,
                                ),
                            createdAt = LocalDateTime.of(2026, 7, 9, 14, 30, 0),
                        ),
                    ),
            )

        mockMvc
            .get("/api/v1/groups/1/parfaits/today") {
                principal = authentication
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.parfaitId") { value(100) }
                jsonPath("$.data.status") { value("ACTIVE") }
                jsonPath("$.data.lastClosedDate") { value("2026-07-08") }
                jsonPath("$.data.groupMembers[0].nickname") { value("연경이") }
                jsonPath("$.data.background.type") { value("COLOR") }
                jsonPath("$.data.images[0].placedBy.nickname") { value("연경이") }
                jsonPath("$.data.images[0].placedBy.nametagChip") { value("TYPE6") }
            }
    }

    @Test
    fun `빈 캔버스면 background와 images가 null로 응답된다`() {
        every { getTodayParfaitUseCase.get(any()) } returns
            GetTodayParfaitResult(
                parfaitId = 100L,
                date = LocalDate.of(2026, 7, 9),
                status = ParfaitStatus.ACTIVE,
                lastClosedDate = null,
                groupMembers = emptyList(),
                background = null,
                images = null,
            )

        mockMvc
            .get("/api/v1/groups/1/parfaits/today") {
                principal = authentication
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.background") { value(nullValue()) }
                jsonPath("$.data.images") { value(nullValue()) }
                jsonPath("$.data.lastClosedDate") { value(nullValue()) }
            }
    }

    @Test
    fun `그룹에 참여하지 않았으면 403과 GROUP_NOT_JOINED로 응답한다 (오늘 조회)`() {
        every { getTodayParfaitUseCase.get(any()) } throws
            ParfaitGroupException(ParfaitGroupError.GROUP_NOT_JOINED)

        mockMvc
            .get("/api/v1/groups/1/parfaits/today") {
                principal = authentication
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.code") { value("GROUP_NOT_JOINED") }
            }
    }

    @Test
    fun `파르페 상세 조회에 성공하면 200과 캔버스 정보를 응답한다`() {
        every { getParfaitDetailUseCase.getDetail(any()) } returns
            GetTodayParfaitResult(
                parfaitId = 98L,
                date = LocalDate.of(2026, 7, 7),
                status = ParfaitStatus.CLOSED,
                lastClosedDate = LocalDate.of(2026, 7, 7),
                groupMembers =
                    listOf(
                        GroupMemberResult(id = 10L, nickname = "연경이", nametagChip = NameTagChipType.TYPE1),
                    ),
                background = BackgroundResult(type = BackgroundType.COLOR, value = "#FFFFFF"),
                images = null,
            )

        mockMvc
            .get("/api/v1/groups/1/parfaits/98") {
                principal = authentication
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.parfaitId") { value(98) }
                jsonPath("$.data.status") { value("CLOSED") }
                jsonPath("$.data.background.type") { value("COLOR") }
            }

        verify {
            getParfaitDetailUseCase.getDetail(
                match { it.memberId == 42L && it.groupId == 1L && it.parfaitId == 98L },
            )
        }
    }

    @Test
    fun `존재하지 않는 파르페면 404와 PARFAIT_NOT_FOUND로 응답한다`() {
        every { getParfaitDetailUseCase.getDetail(any()) } throws
            BusinessException(ParfaitErrorCode.PARFAIT_NOT_FOUND)

        mockMvc
            .get("/api/v1/groups/1/parfaits/98") {
                principal = authentication
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("PARFAIT_NOT_FOUND") }
            }
    }

    @Test
    fun `그룹에 참여하지 않았으면 403과 GROUP_NOT_JOINED로 응답한다 (상세 조회)`() {
        every { getParfaitDetailUseCase.getDetail(any()) } throws
            ParfaitGroupException(ParfaitGroupError.GROUP_NOT_JOINED)

        mockMvc
            .get("/api/v1/groups/1/parfaits/98") {
                principal = authentication
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.code") { value("GROUP_NOT_JOINED") }
            }
    }

    @TestConfiguration
    class UseCaseConfig {
        @Bean
        fun getParfaitYearsUseCase(): GetParfaitYearsUseCase = mockk()

        @Bean
        fun getPastParfaitsUseCase(): GetPastParfaitsUseCase = mockk()

        @Bean
        fun getTodayParfaitUseCase(): GetTodayParfaitUseCase = mockk()

        @Bean
        fun getParfaitDetailUseCase(): GetParfaitDetailUseCase = mockk()
    }
}
