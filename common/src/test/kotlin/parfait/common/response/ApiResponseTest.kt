package parfait.common.response

import parfait.common.error.BaseErrorCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private object TestErrorCode : BaseErrorCode {
    override val status = 400
    override val code = "TEST_ERROR"
    override val message = "테스트 에러 메시지"
}

class ApiResponseTest {
    @Test
    fun `ok는 success true와 code OK를 담는다`() {
        val response = ApiResponse.ok("hello")

        assertTrue(response.success)
        assertEquals("OK", response.code)
        assertEquals("hello", response.data)
        assertNull(response.errorDetail)
    }

    @Test
    fun `created는 success true와 code CREATED를 담는다`() {
        val response = ApiResponse.created("hello")

        assertTrue(response.success)
        assertEquals("CREATED", response.code)
        assertEquals("hello", response.data)
        assertNull(response.errorDetail)
    }

    @Test
    fun `error는 success false와 errorCode 정보를 담는다`() {
        val response = ApiResponse.error(TestErrorCode)

        assertFalse(response.success)
        assertEquals("TEST_ERROR", response.code)
        assertEquals("테스트 에러 메시지", response.message)
        assertNull(response.data)
        assertNull(response.errorDetail)
    }

    @Test
    fun `error는 errorDetail을 담을 수 있다`() {
        val errorDetail = mapOf("field" to "필수 값입니다")

        val response = ApiResponse.error(TestErrorCode, errorDetail)

        assertEquals(errorDetail, response.errorDetail)
    }
}
