package parfait.http.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class TraceIdFilter :
    OncePerRequestFilter(),
    Ordered {
    private val accessLogger = LoggerFactory.getLogger("http.access")

    companion object {
        const val TRACE_ID_HEADER = "X-Trace-Id"
        const val TRACE_ID_MDC_KEY = "traceId"
    }

    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val traceId = UUID.randomUUID().toString()
        val startTime = System.currentTimeMillis()

        try {
            MDC.put(TRACE_ID_MDC_KEY, traceId)
            response.setHeader(TRACE_ID_HEADER, traceId)
            filterChain.doFilter(request, response)
        } finally {
            val elapsed = System.currentTimeMillis() - startTime
            accessLogger.info(
                "{} {} {} {}ms",
                request.method,
                request.requestURI,
                response.status,
                elapsed,
            )
            MDC.remove(TRACE_ID_MDC_KEY)
        }
    }
}
