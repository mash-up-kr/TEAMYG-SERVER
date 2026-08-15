package parfait.core.parfait.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.support.RetryTemplate

@Configuration
class RetryConfig {
    @Bean
    fun canvasRotationRetryTemplate(): RetryTemplate =
        RetryTemplate
            .builder()
            .maxAttempts(3)
            .fixedBackoff(200)
            .build()
}
