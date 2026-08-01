package parfait.http.global.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {
    @Bean
    fun parfaitOpenApi(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("Parfait API")
                    .description("Parfait 서버 API 문서")
                    .version("v1"),
            ).addSecurityItem(SecurityRequirement().addList(BEARER_AUTH_SCHEME))
            .components(
                Components().addSecuritySchemes(
                    BEARER_AUTH_SCHEME,
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"),
                ),
            )

    companion object {
        private const val BEARER_AUTH_SCHEME = "bearerAuth"
    }
}
