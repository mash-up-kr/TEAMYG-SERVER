package parfait.http.image.dto

import jakarta.validation.constraints.NotBlank
import parfait.core.image.domain.ImageType
import parfait.core.image.port.`in`.IssueImageUploadUrlCommand

data class IssueImageUploadUrlRequest(
    @field:NotBlank val fileName: String,
    @field:NotBlank val contentType: String,
    val imageType: ImageType,
) {
    fun toCommand(memberId: Long): IssueImageUploadUrlCommand =
        IssueImageUploadUrlCommand(
            memberId = memberId,
            contentType = contentType,
            imageType = imageType,
        )
}
