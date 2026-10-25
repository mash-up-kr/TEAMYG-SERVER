package parfait.http.parfaitimage.dto

import parfait.core.parfaitimage.port.`in`.UpdateParfaitImagesResult
import parfait.http.parfaittext.dto.UpdateParfaitTextResponse

data class UpdateParfaitImagesResponse(
    val images: List<UpdateParfaitImageResponse>,
    val texts: List<UpdateParfaitTextResponse>,
) {
    companion object {
        fun from(result: UpdateParfaitImagesResult): UpdateParfaitImagesResponse =
            UpdateParfaitImagesResponse(
                images = result.images.map { UpdateParfaitImageResponse.from(it) },
                texts = result.texts.map { UpdateParfaitTextResponse.from(it) },
            )
    }
}
