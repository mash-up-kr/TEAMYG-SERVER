package parfait.http.api.notification.controller

import io.mockk.clearMocks
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
import org.springframework.test.web.servlet.post
import parfait.core.notification.domain.DevicePlatform
import parfait.core.notification.port.`in`.RegisterDeviceTokenCommand
import parfait.core.notification.port.`in`.RegisterDeviceTokenUseCase
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig

@WebMvcTest(controllers = [DeviceTokenController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalExceptionHandler::class,
    TestMemberQueryPortConfig::class,
    TestTokenValidatePortConfig::class,
    DeviceTokenControllerTest.UseCaseConfig::class,
)
class DeviceTokenControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var registerDeviceTokenUseCase: RegisterDeviceTokenUseCase

    private val authentication = UsernamePasswordAuthenticationToken("42", "session-1", emptyList())
    private val authenticationWithoutSession = UsernamePasswordAuthenticationToken("42", null, emptyList())

    @BeforeEach
    fun resetMocks() {
        clearMocks(registerDeviceTokenUseCase)
    }

    @TestConfiguration
    class UseCaseConfig {
        @Bean
        fun registerDeviceTokenUseCase(): RegisterDeviceTokenUseCase = mockk(relaxed = true)
    }

    @Test
    fun `등록 요청이면 credentials의 sessionId를 담아 register를 호출하고 204를 응답한다`() {
        mockMvc
            .post("/api/v1/notifications/devices") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"token":"fcm-token","platform":"IOS"}"""
            }.andExpect {
                status { isNoContent() }
            }

        verify {
            registerDeviceTokenUseCase.register(
                RegisterDeviceTokenCommand(
                    memberId = 42L,
                    sessionId = "session-1",
                    token = "fcm-token",
                    platform = DevicePlatform.IOS,
                ),
            )
        }
    }

    @Test
    fun `credentials에 sessionId가 없으면 command의 sessionId는 null이다`() {
        mockMvc
            .post("/api/v1/notifications/devices") {
                principal = authenticationWithoutSession
                contentType = MediaType.APPLICATION_JSON
                content = """{"token":"fcm-token","platform":"ANDROID"}"""
            }.andExpect {
                status { isNoContent() }
            }

        verify {
            registerDeviceTokenUseCase.register(
                RegisterDeviceTokenCommand(
                    memberId = 42L,
                    sessionId = null,
                    token = "fcm-token",
                    platform = DevicePlatform.ANDROID,
                ),
            )
        }
    }

    @Test
    fun `token이 비어 있으면 400과 INVALID_REQUEST로 응답한다`() {
        mockMvc
            .post("/api/v1/notifications/devices") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"token":"","platform":"IOS"}"""
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("INVALID_REQUEST") }
            }

        verify(exactly = 0) { registerDeviceTokenUseCase.register(any()) }
    }

    @Test
    fun `platform이 허용되지 않는 값이면 400으로 응답한다`() {
        mockMvc
            .post("/api/v1/notifications/devices") {
                principal = authentication
                contentType = MediaType.APPLICATION_JSON
                content = """{"token":"fcm-token","platform":"WINDOWS"}"""
            }.andExpect {
                status { isBadRequest() }
            }

        verify(exactly = 0) { registerDeviceTokenUseCase.register(any()) }
    }
}
