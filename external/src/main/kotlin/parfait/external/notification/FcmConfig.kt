package parfait.external.notification

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ResourceLoader

/**
 * external 의 첫 @Configuration. FirebaseMessaging 을 스프링 빈으로 노출한다.
 * FirebaseApp.initializeApp 은 초기화 시 네트워크 호출을 하지 않는다 — 자격증명은 첫 발송 때 검증된다.
 * 자격증명 파일이 없거나 깨지면 컨텍스트 로딩 실패(fail-fast).
 */
@Configuration
class FcmConfig(
    @Value("\${fcm.credentials-path}") private val credentialsPath: String,
    private val resourceLoader: ResourceLoader,
) {
    @Bean
    fun firebaseMessaging(): FirebaseMessaging {
        val credentials =
            resourceLoader.getResource(credentialsPath).inputStream.use { stream ->
                GoogleCredentials.fromStream(stream)
            }
        val options =
            FirebaseOptions
                .builder()
                .setCredentials(credentials)
                .setConnectTimeout(2_000) // 2026-09-02 방침 4: 짧은 FCM 타임아웃
                .setReadTimeout(3_000)
                .build()
        val app =
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
            } else {
                FirebaseApp.getInstance()
            }
        return FirebaseMessaging.getInstance(app)
    }
}
