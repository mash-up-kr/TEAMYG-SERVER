package parfait.http.filter

import jakarta.servlet.FilterChain
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TraceIdFilterTest {
    @Test
    fun `필터 체인 실행 중에는 MDC와 응답 헤더에 같은 traceId가 있고, 끝나면 MDC가 정리된다`() {
        val filter = TraceIdFilter()
        val request = MockHttpServletRequest("GET", "/health")
        val response = MockHttpServletResponse()
        var traceIdDuringChain: String? = null
        val chain = FilterChain { _, _ -> traceIdDuringChain = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY) }

        filter.doFilter(request, response, chain)

        assertNotNull(traceIdDuringChain)
        assertTrue(traceIdDuringChain!!.isNotBlank())
        val headerValue = response.getHeader(TraceIdFilter.TRACE_ID_HEADER)
        assertEquals(traceIdDuringChain, headerValue)
        assertNull(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY))
    }

    @Test
    fun `필터 체인에서 예외가 발생해도 MDC는 정리된다`() {
        val filter = TraceIdFilter()
        val request = MockHttpServletRequest("GET", "/health")
        val response = MockHttpServletResponse()
        val chain = FilterChain { _, _ -> throw IllegalStateException("boom") }

        assertFailsWith<IllegalStateException> {
            filter.doFilter(request, response, chain)
        }

        assertNull(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY))
    }

    @Test
    fun `서로 다른 요청은 서로 다른 traceId를 받는다`() {
        val filter = TraceIdFilter()
        val traceIds = mutableListOf<String?>()
        val chain = FilterChain { _, _ -> traceIds.add(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY)) }

        filter.doFilter(MockHttpServletRequest("GET", "/health"), MockHttpServletResponse(), chain)
        filter.doFilter(MockHttpServletRequest("GET", "/health"), MockHttpServletResponse(), chain)

        assertEquals(2, traceIds.size)
        assertNotEquals(traceIds[0], traceIds[1])
    }
}
