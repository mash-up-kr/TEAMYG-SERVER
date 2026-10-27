package parfait.bootstrap

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// web/landing 빌드 산출물은 손으로 bootstrap/src/main/resources에 복사한다.
// RootLandingController는 기동할 때 두 HTML을 읽으므로 하나라도 빠지면 앱 전체가 뜨지 않는다.
class LandingResourcesTest {
    private val pages = listOf("/landing/index.html", "/landing/oss/index.html")

    @Test
    fun `랜딩과 오픈소스 라이선스 페이지 HTML이 classpath에 있다`() {
        for (page in pages) {
            assertNotNull(javaClass.getResource(page), "$page 가 없다 — web/landing/dist를 복사할 때 빠졌다")
        }
    }

    @Test
    fun `HTML이 참조하는 assets 파일이 static assets에 있다`() {
        for (page in pages) {
            val html = javaClass.getResource(page)!!.readText()
            val assets = Regex("""(?:src|href)="(/assets/[^"]+)"""").findAll(html).map { it.groupValues[1] }.toList()
            assertTrue(assets.isNotEmpty(), "$page 에서 /assets 참조를 찾지 못했다")
            for (asset in assets) {
                assertNotNull(javaClass.getResource("/static$asset"), "$page 가 참조하는 $asset 가 static에 없다")
            }
        }
    }
}
