@file:Suppress("ktlint:standard:package-name")

package parfait.core.image.port.`in`

import parfait.core.image.domain.ImageType

interface IssueImageUploadUrlUseCase {
    fun issue(command: IssueImageUploadUrlCommand): IssueImageUploadUrlResult
}

data class IssueImageUploadUrlCommand(
    val memberId: Long,
    val contentType: String,
    val imageType: ImageType,
)

data class IssueImageUploadUrlResult(
    val imageId: Long,
    val uploadUrl: String,
    val imageUrl: String,
    val expiresIn: Long,
)
