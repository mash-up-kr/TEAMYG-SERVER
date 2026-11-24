package parfait.core.appversion.service

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import parfait.core.appversion.domain.AppPlatform
import parfait.core.appversion.domain.AppVersion
import parfait.core.appversion.domain.SemVer
import parfait.core.appversion.domain.UpdateType
import parfait.core.appversion.exception.AppVersionErrorCode
import parfait.core.appversion.port.out.AppVersionQueryPort
import parfait.core.exception.BusinessException
import kotlin.test.assertFailsWith

class AppVersionServiceTest {
    private val appVersionQueryPort = mockk<AppVersionQueryPort>()
    private val service = AppVersionService(appVersionQueryPort)

    private fun storedVersion(
        minimum: String = "1.3.0",
        latest: String = "1.5.0",
    ) = AppVersion(
        platform = AppPlatform.IOS,
        minimumVersion = SemVer.parse(minimum),
        latestVersion = SemVer.parse(latest),
        storeUrl = null,
    )

    @Test
    fun `올바른 버전과 플랫폼을 전달하면 업데이트 타입을 반환한다`() {
        every { appVersionQueryPort.findByPlatform(AppPlatform.IOS) } returns storedVersion()

        val result = service.check(AppPlatform.IOS, "1.4.0")

        result.updateType shouldBe UpdateType.OPTIONAL
        result.minimumVersion shouldBe "1.3.0"
        result.latestVersion shouldBe "1.5.0"
        result.storeUrl shouldBe null
    }

    @Test
    fun `버전 형식이 잘못되면 INVALID_VERSION_FORMAT 예외를 던진다`() {
        val ex = assertFailsWith<BusinessException> { service.check(AppPlatform.IOS, "1.a.0") }
        ex.errorCode shouldBe AppVersionErrorCode.INVALID_VERSION_FORMAT
    }

    @Test
    fun `DB 에 플랫폼 레코드가 없으면 APP_VERSION_NOT_FOUND 예외를 던진다`() {
        every { appVersionQueryPort.findByPlatform(AppPlatform.ANDROID) } returns null

        val ex = assertFailsWith<BusinessException> { service.check(AppPlatform.ANDROID, "1.4.0") }
        ex.errorCode shouldBe AppVersionErrorCode.APP_VERSION_NOT_FOUND
    }
}
