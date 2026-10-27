package parfait.http.api.page.root

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.ClassPathResource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.charset.StandardCharsets

@RestController
class RootLandingController(
    @Value("\${landing.domain}") private val landingDomain: String,
) {
    // web/landing(Vite+React) 빌드 산출물. 요청마다 다시 읽지 않도록 기동 시 한 번만 로드한다.
    private val indexHtml: String = readHtml("landing/index.html")

    // 오픈소스 라이선스 페이지. web/landing의 두 번째 진입점(oss/index.html)이다.
    private val ossHtml: String = readHtml("landing/oss/index.html")

    @GetMapping("/")
    fun index(request: HttpServletRequest): ResponseEntity<String> = html(request, indexHtml)

    @GetMapping("/oss", "/oss/")
    fun oss(request: HttpServletRequest): ResponseEntity<String> = html(request, ossHtml)

    private fun html(
        request: HttpServletRequest,
        body: String,
    ): ResponseEntity<String> {
        if (!request.serverName.equals(landingDomain, ignoreCase = true)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
        return ResponseEntity
            .ok()
            .contentType(MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
            .body(body)
    }

    private fun readHtml(path: String): String =
        ClassPathResource(path)
            .inputStream
            .bufferedReader(StandardCharsets.UTF_8)
            .use { it.readText() }
}
