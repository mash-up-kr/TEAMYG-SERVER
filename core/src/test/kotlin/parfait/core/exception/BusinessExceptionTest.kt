package parfait.core.exception

import parfait.common.error.BaseErrorCode
import kotlin.test.Test
import kotlin.test.assertEquals

private object TestErrorCode : BaseErrorCode {
    override val status = 400
    override val code = "TEST_ERROR"
    override val message = "테스트 에러 메시지"
}

class BusinessExceptionTest {
    @Test
    fun `BusinessException은 생성자로 받은 errorCode를 그대로 노출한다`() {
        val exception = BusinessException(TestErrorCode)

        assertEquals(TestErrorCode, exception.errorCode)
        assertEquals("테스트 에러 메시지", exception.message)
    }
}
