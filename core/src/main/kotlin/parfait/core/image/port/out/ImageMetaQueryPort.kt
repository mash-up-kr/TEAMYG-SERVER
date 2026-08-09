package parfait.core.image.port.out

import parfait.core.image.domain.ImageMeta

interface ImageMetaQueryPort {
    fun findById(imageId: Long): ImageMeta?
}
