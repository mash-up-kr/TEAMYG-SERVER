package parfait.http.api.notification.dto

import jakarta.validation.constraints.NotBlank
import parfait.core.notification.domain.DevicePlatform

data class RegisterDeviceTokenRequest(
    @field:NotBlank val token: String,
    val platform: DevicePlatform,
)
