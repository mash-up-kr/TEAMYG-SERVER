package parfait.core.image.port.out

import parfait.core.image.domain.ImageMeta

interface ImageMetaSavePort {
    fun save(imageMeta: ImageMeta): ImageMeta
}
