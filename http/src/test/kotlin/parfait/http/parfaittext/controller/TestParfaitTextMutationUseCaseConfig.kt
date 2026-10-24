package parfait.http.parfaittext.controller

import io.mockk.mockk
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import parfait.core.parfaittext.port.`in`.DeleteParfaitTextUseCase
import parfait.core.parfaittext.port.`in`.UpdateParfaitTextUseCase

@TestConfiguration
class TestParfaitTextMutationUseCaseConfig {
    @Bean
    fun updateParfaitTextUseCase(): UpdateParfaitTextUseCase = mockk()

    @Bean
    fun deleteParfaitTextUseCase(): DeleteParfaitTextUseCase = mockk()
}
