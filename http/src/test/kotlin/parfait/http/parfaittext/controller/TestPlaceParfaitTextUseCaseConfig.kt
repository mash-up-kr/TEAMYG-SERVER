package parfait.http.parfaittext.controller

import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import parfait.core.parfaittext.port.`in`.PlaceParfaitTextUseCase

@TestConfiguration
class TestPlaceParfaitTextUseCaseConfig {
    @Bean
    fun placeParfaitTextUseCase(): PlaceParfaitTextUseCase = mockk()
}
