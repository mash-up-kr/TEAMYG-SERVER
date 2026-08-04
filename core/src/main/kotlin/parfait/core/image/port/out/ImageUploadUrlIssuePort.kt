package parfait.core.image.port.out

interface ImageUploadUrlIssuePort {
    fun issue(
        key: String,
        contentType: String,
        expirationSeconds: Long,
    ): PresignedUpload
}

data class PresignedUpload(
    val uploadUrl: String,
    val imageUrl: String,
)
