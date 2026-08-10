package parfait.core.parfaitimage.port.out

import parfait.core.parfaitimage.domain.ParfaitImage

interface ParfaitImageSavePort {
    fun save(parfaitImage: ParfaitImage): ParfaitImage
}
