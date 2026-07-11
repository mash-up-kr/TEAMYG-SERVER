package parfait.http.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun parfaitOpenApi(): OpenAPI =
        OpenAPI().info(
            Info()
                .title("Parfait API")
                .description("Parfait 서버 API 문서")
                .version("v1"),
        )
}
