package parfait.http.global.security

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.servlet.HandlerExceptionResolver
import parfait.core.auth.exception.AuthErrorCode
import parfait.core.exception.BusinessException

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthFilter,
    @Qualifier("handlerExceptionResolver") private val resolver: HandlerExceptionResolver,
) {
    companion object {
        private val WHITELIST_PATHS =
            arrayOf(
                "/actuator/health",
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/favicon.ico",
                "/v3/api-docs/**",
                "/api/v1/auth/kakao",
                "/api/v1/auth/signup",
                "/api/v1/auth/reissue",
                "/api/v1/policies",
            )
    }

    @Bean
    fun authenticationEntryPoint(): AuthenticationEntryPoint =
        AuthenticationEntryPoint { request, response, _ ->
            resolver.resolveException(request, response, null, BusinessException(AuthErrorCode.UNAUTHORIZED))
        }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling { it.authenticationEntryPoint(authenticationEntryPoint()) }
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers(*WHITELIST_PATHS)
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }.addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }
}
