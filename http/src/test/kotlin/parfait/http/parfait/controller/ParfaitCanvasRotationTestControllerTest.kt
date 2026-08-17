package parfait.http.parfait.controller

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import parfait.core.parfait.port.`in`.ForceRotateParfaitCanvasesUseCase
import parfait.core.parfait.port.`in`.RotateParfaitCanvasesResult
import parfait.http.global.exception.GlobalExceptionHandler
import parfait.http.global.security.TestMemberQueryPortConfig
import parfait.http.global.security.TestTokenValidatePortConfig

@WebMvcTest(controllers = [ParfaitCanvasRotationTestController::class])
@AutoConfigureMockMvc(addFilters = false)
@Import(
    GlobalExceptionHandler::class,
    ParfaitCanvasRotationTestControllerTest.UseCaseConfig::class,
    TestMemberQueryPortConfig::class,
    TestTokenValidatePortConfig::class,
)
class ParfaitCanvasRotationTestControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var forceRotateParfaitCanvasesUseCase: ForceRotateParfaitCanvasesUseCase

    @Test
    fun `호출하면 UseCase 결과를 응답 필드로 매핑한다`() {
        every { forceRotateParfaitCanvasesUseCase.forceRotateAll() } returns
            RotateParfaitCanvasesResult(closedCount = 2, emptyCount = 1, createdCount = 3, failedCount = 0)

        mockMvc.post("/api/v1/test/parfait-canvas/rotate").andExpect {
            status { isOk() }
            jsonPath("$.data.closedCount") { value(2) }
            jsonPath("$.data.emptyCount") { value(1) }
            jsonPath("$.data.createdCount") { value(3) }
            jsonPath("$.data.failedCount") { value(0) }
        }
    }

    @TestConfiguration
    class UseCaseConfig {
        @Bean
        fun forceRotateParfaitCanvasesUseCase(): ForceRotateParfaitCanvasesUseCase = mockk()
    }
}
