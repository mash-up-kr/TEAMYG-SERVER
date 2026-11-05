package parfait.http.api.notification.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import parfait.common.response.ApiResponse
import parfait.core.notification.port.`in`.GetNotificationSettingsUseCase
import parfait.core.notification.port.`in`.UpdateNotificationSettingsCommand
import parfait.core.notification.port.`in`.UpdateNotificationSettingsUseCase
import parfait.http.api.notification.dto.NotificationSettingsResponse
import parfait.http.api.notification.dto.UpdateNotificationSettingsRequest

@Tag(name = "Notification")
@RestController
@RequestMapping("/api/v1/notifications/settings")
class NotificationSettingsController(
    private val getNotificationSettingsUseCase: GetNotificationSettingsUseCase,
    private val updateNotificationSettingsUseCase: UpdateNotificationSettingsUseCase,
) {
    @Operation(summary = "알림 설정 조회(오늘의 캔버스 알림, 알림 모아 받기)")
    @GetMapping
    fun get(authentication: Authentication): ApiResponse<NotificationSettingsResponse> =
        ApiResponse.ok(
            NotificationSettingsResponse.from(getNotificationSettingsUseCase.get(authentication.name.toLong())),
        )

    @Operation(summary = "알림 설정 변경(보낸 항목만 변경)")
    @PatchMapping
    fun update(
        authentication: Authentication,
        @RequestBody request: UpdateNotificationSettingsRequest,
    ): ApiResponse<NotificationSettingsResponse> =
        ApiResponse.ok(
            NotificationSettingsResponse.from(
                updateNotificationSettingsUseCase.update(
                    UpdateNotificationSettingsCommand(
                        memberId = authentication.name.toLong(),
                        dailyReminderEnabled = request.dailyReminderEnabled,
                        activityDigestEnabled = request.activityDigestEnabled,
                    ),
                ),
            ),
        )
}
