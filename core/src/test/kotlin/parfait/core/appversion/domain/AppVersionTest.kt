package parfait.core.appversion.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class AppVersionTest {
    private fun appVersion(
        minimum: String = "1.3.0",
        latest: String = "1.5.0",
    ) = AppVersion(
        platform = AppPlatform.IOS,
        minimumVersion = SemVer.parse(minimum),
        latestVersion = SemVer.parse(latest),
        storeUrl = null,
    )

    @Test
    fun `현재 버전이 최소 버전보다 낮으면 FORCE 를 반환한다`() {
        appVersion(minimum = "1.3.0").checkUpdateType(SemVer.parse("1.2.9")) shouldBe UpdateType.FORCE
    }

    @Test
    fun `현재 버전이 최소 버전과 같으면 OPTIONAL 을 반환한다`() {
        appVersion(minimum = "1.3.0", latest = "1.5.0").checkUpdateType(SemVer.parse("1.3.0")) shouldBe
            UpdateType.OPTIONAL
    }

    @Test
    fun `현재 버전이 최신 버전보다 낮으면 OPTIONAL 을 반환한다`() {
        appVersion(minimum = "1.3.0", latest = "1.5.0").checkUpdateType(SemVer.parse("1.4.0")) shouldBe
            UpdateType.OPTIONAL
    }

    @Test
    fun `현재 버전이 최신 버전과 같으면 NONE 을 반환한다`() {
        appVersion(latest = "1.5.0").checkUpdateType(SemVer.parse("1.5.0")) shouldBe UpdateType.NONE
    }

    @Test
    fun `현재 버전이 최신 버전보다 높으면 NONE 을 반환한다`() {
        appVersion(latest = "1.5.0").checkUpdateType(SemVer.parse("1.6.0")) shouldBe UpdateType.NONE
    }
}
