package parfait.external.apple

import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import parfait.core.auth.exception.AuthErrorCode
import parfait.core.auth.port.out.AppleAuthorizationCodeExchangePort
import parfait.core.exception.BusinessException
import java.time.Duration

@Component
class AppleAuthorizationCodeExchangeAdapter(
    private val appleClientSecretGenerator: AppleClientSecretGenerator,
    @Value("\${apple.client-id}") private val clientId: String,
    @Value("\${apple.token-uri}") private val tokenUri: String,
) : AppleAuthorizationCodeExchangePort {
    private val log = LoggerFactory.getLogger(AppleAuthorizationCodeExchangeAdapter::class.java)
    private val restClient: RestClient =
        RestClient
            .builder()
            .requestFactory(
                SimpleClientHttpRequestFactory().apply {
                    setConnectTimeout(CONNECT_TIMEOUT)
                    setReadTimeout(READ_TIMEOUT)
                },
            ).build()

    override fun exchange(authorizationCode: String): String {
        val clientSecret = appleClientSecretGenerator.generate()
        val form =
            LinkedMultiValueMap<String, String>().apply {
                add("grant_type", "authorization_code")
                add("code", authorizationCode)
                add("client_id", clientId)
                add("client_secret", clientSecret)
            }

        val response =
            try {
                restClient
                    .post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(AppleTokenResponse::class.java)
            } catch (e: HttpClientErrorException) {
                log.warn("애플 authorizationCode 교환 거부: {}", e.responseBodyAsString, e)
                throw BusinessException(AuthErrorCode.INVALID_ID_TOKEN)
            } catch (e: HttpServerErrorException) {
                log.warn("애플 토큰 서버 응답 오류", e)
                throw BusinessException(AuthErrorCode.APPLE_SERVER_ERROR)
            } catch (e: ResourceAccessException) {
                log.warn("애플 토큰 서버 연결 실패", e)
                throw BusinessException(AuthErrorCode.APPLE_SERVER_UNAVAILABLE)
            } catch (e: RestClientException) {
                log.warn("애플 토큰 서버 응답 파싱 실패", e)
                throw BusinessException(AuthErrorCode.APPLE_SERVER_ERROR)
            }

        return response?.refreshToken ?: run {
            log.warn("애플 토큰 응답에 refresh_token이 없습니다")
            throw BusinessException(AuthErrorCode.APPLE_SERVER_ERROR)
        }
    }

    companion object {
        private val CONNECT_TIMEOUT = Duration.ofSeconds(5)
        private val READ_TIMEOUT = Duration.ofSeconds(10)
    }
}

data class AppleTokenResponse(
    @JsonProperty("refresh_token") val refreshToken: String?,
)
