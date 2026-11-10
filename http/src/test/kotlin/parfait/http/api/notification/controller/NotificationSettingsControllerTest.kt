package parfait.http.api.notification.controller

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import parfait.core.notification.domain.NotificationSettings
import parfait.core.notification.port.`in`.GetNotificationSettingsUseCase
import parfait.core.notification.port.`in`.UpdateNotificationSettingsCommand
import parfait.core.notification.port.`in`.UpdateNotificationSettingsUseCase
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig

@WebMvcTest(controllers = [NotificationSettingsController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalExceptionHandler::class,
    TestMemberQueryPortConfig::class,
    TestTokenValidatePortConfig::class,
    NotificationSettingsControllerTest.UseCaseConfig::class,
)
class NotificationSettingsControllerTest {
    @Autowired lateinit var mockMvc: MockMvc

    @Autowired lateinit var getUseCase: GetNotificationSettingsUseCase

    @Autowired lateinit var updateUseCase: UpdateNotificationSettingsUseCase

    private val authentication = UsernamePasswordAuthenticationToken("42", "session-1", emptyList())

    @BeforeEach
    fun resetMocks() {
        clearMocks(getUseCase, updateUseCase)
    }

    @TestConfiguration
    class UseCaseConfig {
        @Bean
        fun getNotificationSettingsUseCase(): GetNotificationSettingsUseCase = mockk()

        @Bean
        fun updateNotificationSettingsUseCase(): UpdateNotificationSettingsUseCase = mockk()
    }

    @Test
    fun `조회하면 인증 회원의 두 설정값을 응답한다`() {
        every { getUseCase.get(42L) } returns
            NotificationSettings(dailyReminderEnabled = true, activityDigestEnabled = false)

        mockMvc
            .get("/api/v1/notifications/settings") {
                principal = authentication
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.dailyReminderEnabled") { value(true) }
                jsonPath("$.data.activityDigestEnabled") { value(false) }
            }
    }

    @Test
    fun `변경하면 보낸 항목만 담아 유스케이스를 호출하고 바뀐 설정을 응답한다`() {
        every { updateUseCase.update(any()) } returns
            NotificationSettings(dailyReminderEnabled = true, activityDigestEnabled = true)

        mockMvc
            .patch("/api/v1/notifications/settings") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"activityDigestEnabled":true}"""
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.activityDigestEnabled") { value(true) }
            }

        verify {
            updateUseCase.update(
                UpdateNotificationSettingsCommand(
                    memberId = 42L,
                    dailyReminderEnabled = null,
                    activityDigestEnabled = true,
                ),
            )
        }
    }
}
