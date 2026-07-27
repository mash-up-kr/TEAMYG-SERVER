package parfait.external.logging

import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class DiscordWebhookSender(
    private val httpClient: HttpClient =
        HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build(),
) {
    private val log = LoggerFactory.getLogger(DiscordWebhookSender::class.java)

    /**
     * 이 함수 안의 실패 로그는 반드시 WARN 이하로 유지해야 한다 — DiscordErrorAppender는
     * ERROR 레벨 로그에만 반응하므로, 여기서 ERROR로 로깅하면 "전송 실패 → ERROR 로그 → 다시
     * 전송 시도 → 다시 실패" 형태의 무한 피드백 루프가 생길 수 있다.
     */
    fun send(
        webhookUrl: String,
        jsonPayload: String,
    ) {
        try {
            val request =
                HttpRequest
                    .newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(3))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.discarding())
            if (response.statusCode() >= 300) {
                log.warn("Discord webhook 응답 실패: status={}", response.statusCode())
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            log.warn("Discord webhook 전송 중 인터럽트 발생", e)
        } catch (e: Exception) {
            log.warn("Discord webhook 전송 실패", e)
        }
    }
}
