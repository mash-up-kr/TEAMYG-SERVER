package parfait.http.api.stats.controller

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import parfait.core.exception.BusinessException
import parfait.core.stats.exception.StatsErrorCode

class StatsTokenVerifierTest {
    private fun assertRejected(
        verifier: StatsTokenVerifier,
        token: String?,
    ) {
        val ex = shouldThrow<BusinessException> { verifier.verify(token) }
        ex.errorCode shouldBe StatsErrorCode.INVALID_STATS_TOKEN
    }

    @Test
    fun `설정값이 빈 문자열이면 어떤 토큰이든 거절한다`() {
        val verifier = StatsTokenVerifier("")

        listOf(null, "", " ", "anything").forEach { assertRejected(verifier, it) }
    }

    @Test
    fun `설정값이 공백뿐이어도 어떤 토큰이든 거절한다`() {
        val verifier = StatsTokenVerifier("   ")

        listOf(null, "", " ", "   ", "anything").forEach { assertRejected(verifier, it) }
    }

    @Test
    fun `설정값이 있고 토큰이 일치하면 예외가 없다`() {
        StatsTokenVerifier("secret-token").verify("secret-token")
    }

    @Test
    fun `설정값이 있어도 토큰이 없거나 다르면 거절한다`() {
        val verifier = StatsTokenVerifier("secret-token")

        listOf(null, "wrong", "SECRET-TOKEN").forEach { assertRejected(verifier, it) }
    }
}
