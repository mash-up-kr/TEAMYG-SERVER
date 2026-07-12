package parfait.persistence.repository

import org.springframework.data.jpa.repository.JpaRepository
import parfait.persistence.entity.ImageMeta

interface ImageMetaRepository : JpaRepository<ImageMeta, Long>
