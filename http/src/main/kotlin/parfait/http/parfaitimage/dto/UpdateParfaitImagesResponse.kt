package parfait.http.parfaitimage.dto

import parfait.core.parfaitimage.port.`in`.UpdateParfaitImageResult

data class UpdateParfaitImagesResponse(
    val images: List<UpdateParfaitImageResponse>,
) {
    companion object {
        fun from(results: List<UpdateParfaitImageResult>): UpdateParfaitImagesResponse =
            UpdateParfaitImagesResponse(images = results.map { UpdateParfaitImageResponse.from(it) })
    }
}
