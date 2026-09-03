package parfait.core.notification.domain

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MulticastResultTest {
    @Test
    fun `성공·실패 개수와 죽은 토큰을 집계한다`() {
        val result =
            MulticastResult(
                listOf(
                    TokenSendResult("ok-1", success = true, errorCode = null),
                    TokenSendResult("dead-1", success = false, errorCode = "UNREGISTERED"),
                    TokenSendResult("retry-1", success = false, errorCode = "UNAVAILABLE"),
                    TokenSendResult("dead-2", success = false, errorCode = "INVALID_ARGUMENT"),
                ),
            )

        result.successCount shouldBe 1
        result.failureCount shouldBe 3
        result.deadTokens() shouldContainExactly listOf("dead-1", "dead-2")
    }

    @Test
    fun `빈 결과`() {
        val result = MulticastResult(emptyList())

        result.successCount shouldBe 0
        result.failureCount shouldBe 0
        result.deadTokens() shouldBe emptyList()
    }
}
