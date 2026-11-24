package parfait.http.api.appversion.controller

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import parfait.core.appversion.domain.AppPlatform
import parfait.core.appversion.domain.UpdateType
import parfait.core.appversion.exception.AppVersionErrorCode
import parfait.core.appversion.port.`in`.AppVersionCheckResult
import parfait.core.appversion.port.`in`.CheckAppVersionUseCase
import parfait.core.exception.BusinessException
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig

@WebMvcTest(controllers = [AppVersionController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalExceptionHandler::class,
    AppVersionControllerTest.UseCaseConfig::class,
    TestMemberQueryPortConfig::class,
    TestTokenValidatePortConfig::class,
)
class AppVersionControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var checkAppVersionUseCase: CheckAppVersionUseCase

    @BeforeEach
    fun resetMocks() {
        clearMocks(checkAppVersionUseCase)
    }

    @Test
    fun `platform 과 version 으로 버전 체크 결과를 반환한다`() {
        every {
            checkAppVersionUseCase.check(AppPlatform.IOS, "1.4.0")
        } returns
            AppVersionCheckResult(
                updateType = UpdateType.OPTIONAL,
                minimumVersion = "1.3.0",
                latestVersion = "1.5.0",
                storeUrl = null,
            )

        mockMvc.get("/api/v1/app-versions/check?platform=IOS&version=1.4.0").andExpect {
            status { isOk() }
            jsonPath("$.data.updateType") { value("OPTIONAL") }
            jsonPath("$.data.minimumVersion") { value("1.3.0") }
            jsonPath("$.data.latestVersion") { value("1.5.0") }
        }
    }

    @Test
    fun `storeUrl 이 있으면 응답에 포함된다`() {
        every {
            checkAppVersionUseCase.check(AppPlatform.ANDROID, "1.2.0")
        } returns
            AppVersionCheckResult(
                updateType = UpdateType.FORCE,
                minimumVersion = "1.3.0",
                latestVersion = "1.5.0",
                storeUrl = "https://play.google.com/store/apps/details?id=com.example",
            )

        mockMvc.get("/api/v1/app-versions/check?platform=ANDROID&version=1.2.0").andExpect {
            status { isOk() }
            jsonPath("$.data.updateType") { value("FORCE") }
            jsonPath("$.data.storeUrl") { value("https://play.google.com/store/apps/details?id=com.example") }
        }
    }

    @Test
    fun `버전 형식이 잘못되면 400 을 반환한다`() {
        every {
            checkAppVersionUseCase.check(AppPlatform.IOS, "invalid")
        } throws BusinessException(AppVersionErrorCode.INVALID_VERSION_FORMAT)

        mockMvc.get("/api/v1/app-versions/check?platform=IOS&version=invalid").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("INVALID_VERSION_FORMAT") }
        }
    }

    @Test
    fun `platform 파라미터가 없으면 400 을 반환한다`() {
        mockMvc.get("/api/v1/app-versions/check?version=1.4.0").andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `version 파라미터가 없으면 400 을 반환한다`() {
        mockMvc.get("/api/v1/app-versions/check?platform=IOS").andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `알 수 없는 platform 이면 400 을 반환한다`() {
        mockMvc.get("/api/v1/app-versions/check?platform=WINDOWS&version=1.4.0").andExpect {
            status { isBadRequest() }
        }
    }

    @TestConfiguration
    class UseCaseConfig {
        @Bean
        fun checkAppVersionUseCase(): CheckAppVersionUseCase = mockk()
    }
}
