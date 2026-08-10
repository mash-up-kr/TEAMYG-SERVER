package parfait.http.parfaitimage.dto

import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageBorderResult

data class UpdateParfaitImageBorderResponse(
    val parfaitImageId: Long,
    val borderType: String,
    val borderColor: String?,
    val borderWidth: Double?,
) {
    companion object {
        fun from(result: UpdateParfaitImageBorderResult): UpdateParfaitImageBorderResponse =
            UpdateParfaitImageBorderResponse(
                parfaitImageId = result.parfaitImageId,
                borderType = result.borderType.name,
                borderColor = result.borderColor,
                borderWidth = result.borderWidth,
            )
    }
}
