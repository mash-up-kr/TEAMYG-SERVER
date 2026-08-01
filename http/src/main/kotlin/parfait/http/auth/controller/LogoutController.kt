package parfait.http.auth.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import parfait.core.auth.port.`in`.LogoutUseCase
import parfait.http.auth.dto.LogoutRequest

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/v1/auth/logout")
class LogoutController(
    private val logoutUseCase: LogoutUseCase,
) {
    @Operation(summary = "로그아웃")
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun logout(
        authentication: Authentication,
        @Valid @RequestBody request: LogoutRequest,
    ) {
        logoutUseCase.logout(authentication.name.toLong(), request.refreshToken)
    }
}
