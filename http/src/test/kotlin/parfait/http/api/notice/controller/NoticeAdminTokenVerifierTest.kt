package parfait.http.api.notice.controller

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import parfait.core.exception.BusinessException
import parfait.core.notice.exception.NoticeErrorCode

class NoticeAdminTokenVerifierTest {
    private fun assertRejected(
        verifier: NoticeAdminTokenVerifier,
        token: String?,
    ) {
        val ex = shouldThrow<BusinessException> { verifier.verify(token) }
        ex.errorCode shouldBe NoticeErrorCode.INVALID_NOTICE_TOKEN
    }

    @Test
    fun `설정값이 비어 있으면 어떤 토큰이든 거절한다`() {
        listOf("", "   ").forEach { configured ->
            val verifier = NoticeAdminTokenVerifier(configured)
            listOf(null, "", " ", "anything").forEach { assertRejected(verifier, it) }
        }
    }

    @Test
    fun `토큰이 일치하면 예외가 없다`() {
        NoticeAdminTokenVerifier("secret-token").verify("secret-token")
    }

    @Test
    fun `토큰이 없거나 다르거나 빈 문자열이면 거절한다`() {
        val verifier = NoticeAdminTokenVerifier("secret-token")

        listOf(null, "", "wrong", "SECRET-TOKEN").forEach { assertRejected(verifier, it) }
    }
}
