package parfait.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import parfait.core.appversion.domain.AppPlatform

@Entity
@Table(name = "app_version")
class AppVersionEntity(
    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 10)
    var platform: AppPlatform,
    @Column(name = "minimum_version", nullable = false, length = 20)
    var minimumVersion: String,
    @Column(name = "latest_version", nullable = false, length = 20)
    var latestVersion: String,
    @Column(name = "store_url", length = 500)
    var storeUrl: String?,
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long? = null,
)
