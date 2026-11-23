package parfait.http.api.appversion.controller

import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import parfait.core.appversion.port.`in`.CheckAppVersionUseCase

@TestConfiguration
class TestCheckAppVersionUseCaseConfig {
    @Bean
    fun checkAppVersionUseCase(): CheckAppVersionUseCase = mockk(relaxed = true)
}
