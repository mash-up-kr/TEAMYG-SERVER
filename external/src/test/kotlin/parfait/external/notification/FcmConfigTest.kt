package parfait.external.notification

import com.google.firebase.messaging.FirebaseMessaging
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.springframework.core.io.DefaultResourceLoader

class FcmConfigTest {
    private val config =
        FcmConfig(
            credentialsPath = "classpath:fcm/local-firebase-key.json",
            resourceLoader = DefaultResourceLoader(),
        )

    @Test
    fun `가짜 키 리소스로 firebaseMessaging 빈을 만든다`() {
        config.firebaseMessaging().shouldBeInstanceOf<FirebaseMessaging>()
    }

    @Test
    fun `두 번 호출해도 FirebaseApp 중복 초기화 예외가 없다`() {
        config.firebaseMessaging()
        config.firebaseMessaging().shouldBeInstanceOf<FirebaseMessaging>()
    }
}
