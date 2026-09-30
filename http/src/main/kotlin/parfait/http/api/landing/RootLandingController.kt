package parfait.http.api.landing

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Value
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
    @GetMapping("/")
    fun index(request: HttpServletRequest): ResponseEntity<String> {
        if (!request.serverName.equals(landingDomain, ignoreCase = true)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build()
        }
        return ResponseEntity
            .ok()
            .contentType(MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8))
            .body("hello world")
    }
}
