package parfait.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.DynamicUpdate
import java.time.LocalDateTime

/**
 * 단일 행(id=1) 설정 테이블. 행은 V24 마이그레이션이 넣는다.
 * 정기 전송(last_sent_at)과 /광고모드(ad_mode)가 동시에 갱신해도 서로의 컬럼을 덮어쓰지 않도록
 * 변경된 컬럼만 UPDATE한다.
 */
@DynamicUpdate
@Entity
@Table(name = "report_settings")
class ReportSetting(
    @Column(name = "ad_mode", nullable = false)
    var adMode: Boolean = false,
    @Column(name = "interval_days", nullable = false)
    var intervalDays: Int = 3,
    @Column(name = "last_sent_at")
    var lastSentAt: LocalDateTime? = null,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    @Id
    @Column(name = "id")
    var id: Long = SETTINGS_ID,
) {
    companion object {
        const val SETTINGS_ID = 1L
    }
}
