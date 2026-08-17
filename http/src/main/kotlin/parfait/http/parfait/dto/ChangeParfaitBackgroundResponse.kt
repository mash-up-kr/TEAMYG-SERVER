package parfait.http.parfait.dto

import parfait.core.parfait.port.`in`.BackgroundResult

data class ChangeParfaitBackgroundResponse(
    val background: BackgroundResponse,
) {
    companion object {
        fun from(result: BackgroundResult): ChangeParfaitBackgroundResponse =
            ChangeParfaitBackgroundResponse(background = BackgroundResponse.from(result))
    }
}
