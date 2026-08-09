package parfait.http.image.dto

import parfait.core.image.port.`in`.IssueImageUploadUrlResult

data class IssueImageUploadUrlResponse(
    val imageId: Long,
    val uploadUrl: String,
    val imageUrl: String,
    val expiresIn: Long,
) {
    companion object {
        fun from(result: IssueImageUploadUrlResult): IssueImageUploadUrlResponse =
            IssueImageUploadUrlResponse(
                imageId = result.imageId,
                uploadUrl = result.uploadUrl,
                imageUrl = result.imageUrl,
                expiresIn = result.expiresIn,
            )
    }
}
