package parfait.bootstrap

import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.core.io.ClassPathResource
import kotlin.test.Test
import kotlin.test.assertEquals

class KakaoConfigurationTest {
    @Test
    fun `공통 설정은 카카오 issuer·jwks-uri와 registration 토큰 만료시간을 정의한다`() {
        val properties = loadProperties("application.yaml")

        assertEquals("https://kauth.kakao.com", properties.getProperty("kakao.issuer"))
        assertEquals(
            "https://kauth.kakao.com/.well-known/jwks.json",
            properties.getProperty("kakao.jwks-uri"),
        )
        assertEquals(600, properties.getProperty("jwt.registration-token-expiration-seconds"))
    }

    @Test
    fun `dev와 prod 프로필은 kakao app-key를 환경변수로 위임한다`() {
        val dev = loadProperties("application-dev.yaml")
        val prod = loadProperties("application-prod.yaml")

        assertEquals("\${KAKAO_APP_KEY}", dev.getProperty("kakao.app-key"))
        assertEquals("\${KAKAO_APP_KEY}", prod.getProperty("kakao.app-key"))
    }

    private fun loadProperties(fileName: String) =
        YamlPropertySourceLoader()
            .load(fileName, ClassPathResource(fileName))
            .single()
}
