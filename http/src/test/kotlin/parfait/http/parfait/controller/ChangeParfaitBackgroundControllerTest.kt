package parfait.http.parfait.controller

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import org.springframework.test.web.servlet.patch
import parfait.core.exception.BusinessException
import parfait.core.image.exception.ImageErrorCode
import parfait.core.parfait.domain.BackgroundType
import parfait.core.parfait.exception.ParfaitErrorCode
import parfait.core.parfait.port.`in`.BackgroundResult
import parfait.core.parfait.port.`in`.ChangeParfaitBackgroundUseCase
import parfait.core.parfaitgroup.domain.ParfaitGroupError
import parfait.core.parfaitgroup.domain.ParfaitGroupException
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig

@WebMvcTest(controllers = [ChangeParfaitBackgroundController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalExceptionHandler::class,
    ChangeParfaitBackgroundControllerTest.UseCaseConfig::class,
    TestMemberQueryPortConfig::class,
    TestTokenValidatePortConfig::class,
)
class ChangeParfaitBackgroundControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var changeParfaitBackgroundUseCase: ChangeParfaitBackgroundUseCase

    private val authentication = UsernamePasswordAuthenticationToken("42", null, emptyList())

    @Test
    fun `단색 배경 변경에 성공하면 200과 변경된 배경을 응답한다`() {
        every { changeParfaitBackgroundUseCase.change(any()) } returns
            BackgroundResult(type = BackgroundType.COLOR, value = "#FF5733")

        mockMvc
            .patch("/api/v1/groups/1/parfaits/98/background") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"type": "COLOR", "value": "#FF5733"}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.background.type") { value("COLOR") }
                jsonPath("$.data.background.value") { value("#FF5733") }
            }

        verify {
            changeParfaitBackgroundUseCase.change(
                match { it.memberId == 42L && it.groupId == 1L && it.parfaitId == 98L && it.value == "#FF5733" },
            )
        }
    }

    @Test
    fun `이미지 배경 변경에 성공하면 200과 변경된 배경을 응답한다`() {
        every { changeParfaitBackgroundUseCase.change(any()) } returns
            BackgroundResult(type = BackgroundType.IMAGE, value = "https://s3.example/background/80.png")

        mockMvc
            .patch("/api/v1/groups/1/parfaits/98/background") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"type": "IMAGE", "imageId": 80}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.background.type") { value("IMAGE") }
                jsonPath("$.data.background.value") { value("https://s3.example/background/80.png") }
            }

        verify {
            changeParfaitBackgroundUseCase.change(
                match { it.imageId == 80L },
            )
        }
    }

    @Test
    fun `필수 값이 없으면 400과 INVALID_BACKGROUND로 응답한다`() {
        every { changeParfaitBackgroundUseCase.change(any()) } throws
            BusinessException(ParfaitErrorCode.INVALID_BACKGROUND)

        mockMvc
            .patch("/api/v1/groups/1/parfaits/98/background") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"type": "COLOR"}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("INVALID_BACKGROUND") }
            }
    }

    @Test
    fun `존재하지 않는 이미지면 404와 IMAGE_NOT_FOUND로 응답한다`() {
        every { changeParfaitBackgroundUseCase.change(any()) } throws
            BusinessException(ImageErrorCode.IMAGE_NOT_FOUND)

        mockMvc
            .patch("/api/v1/groups/1/parfaits/98/background") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"type": "IMAGE", "imageId": 999}"""
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("IMAGE_NOT_FOUND") }
            }
    }

    @Test
    fun `업로드가 확인되지 않은 이미지면 409와 BACKGROUND_IMAGE_NOT_CONFIRMED로 응답한다`() {
        every { changeParfaitBackgroundUseCase.change(any()) } throws
            BusinessException(ParfaitErrorCode.BACKGROUND_IMAGE_NOT_CONFIRMED)

        mockMvc
            .patch("/api/v1/groups/1/parfaits/98/background") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"type": "IMAGE", "imageId": 80}"""
            }.andExpect {
                status { isConflict() }
                jsonPath("$.code") { value("BACKGROUND_IMAGE_NOT_CONFIRMED") }
            }
    }

    @Test
    fun `존재하지 않는 파르페면 404와 PARFAIT_NOT_FOUND로 응답한다`() {
        every { changeParfaitBackgroundUseCase.change(any()) } throws
            BusinessException(ParfaitErrorCode.PARFAIT_NOT_FOUND)

        mockMvc
            .patch("/api/v1/groups/1/parfaits/98/background") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"type": "COLOR", "value": "#FF5733"}"""
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("PARFAIT_NOT_FOUND") }
            }
    }

    @Test
    fun `그룹에 참여하지 않았으면 403과 GROUP_NOT_JOINED로 응답한다`() {
        every { changeParfaitBackgroundUseCase.change(any()) } throws
            ParfaitGroupException(ParfaitGroupError.GROUP_NOT_JOINED)

        mockMvc
            .patch("/api/v1/groups/1/parfaits/98/background") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"type": "COLOR", "value": "#FF5733"}"""
            }.andExpect {
                status { isForbidden() }
                jsonPath("$.code") { value("GROUP_NOT_JOINED") }
            }
    }

    @TestConfiguration
    class UseCaseConfig {
        @Bean
        fun changeParfaitBackgroundUseCase(): ChangeParfaitBackgroundUseCase = mockk()
    }
}
