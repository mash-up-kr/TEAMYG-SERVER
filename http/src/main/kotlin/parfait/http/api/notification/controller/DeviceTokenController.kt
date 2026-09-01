package parfait.http.api.notification.controller

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
import parfait.core.notification.port.`in`.RegisterDeviceTokenCommand
import parfait.core.notification.port.`in`.RegisterDeviceTokenUseCase
import parfait.http.api.notification.dto.RegisterDeviceTokenRequest

@Tag(name = "Notification")
@RestController
@RequestMapping("/api/v1/notifications/devices")
class DeviceTokenController(
    private val registerDeviceTokenUseCase: RegisterDeviceTokenUseCase,
) {
    @Operation(summary = "기기(FCM) 토큰 등록/갱신")
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun register(
        authentication: Authentication,
        @Valid @RequestBody request: RegisterDeviceTokenRequest,
    ) {
        registerDeviceTokenUseCase.register(
            RegisterDeviceTokenCommand(
                memberId = authentication.name.toLong(),
                sessionId = authentication.credentials as? String,
                token = request.token,
                platform = request.platform,
            ),
        )
    }
}
