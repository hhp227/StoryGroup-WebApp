package kr.hhp227.groupsns_webapp.auth.google

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhp227.groupsns_webapp.common.exception.InvalidGoogleTokenException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Component
class GoogleHttpCodeExchanger(
    private val properties: GoogleOAuthProperties,
    private val objectMapper: ObjectMapper
) : GoogleCodeExchanger {
    private val log = LoggerFactory.getLogger(GoogleHttpCodeExchanger::class.java)
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()

    override fun exchange(code: String, codeVerifier: String, redirectUri: String): String {
        if (properties.desktopClientId.isEmpty() || properties.desktopClientSecret.isEmpty()) {
            throw InvalidGoogleTokenException("구글 로그인이 설정되지 않았습니다")
        }
        val form = mapOf(
            "grant_type" to "authorization_code",
            "code" to code,
            "code_verifier" to codeVerifier,
            "redirect_uri" to redirectUri,
            "client_id" to properties.desktopClientId,
            "client_secret" to properties.desktopClientSecret
        ).entries.joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, Charsets.UTF_8)}" }
        val request = HttpRequest.newBuilder(URI.create(TOKEN_ENDPOINT))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(form))
            .build()
        val response = try {
            http.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            log.warn("구글 코드 교환 요청 실패: {}", e.message)
            throw InvalidGoogleTokenException()
        }
        if (response.statusCode() != 200) {
            // 본문에 error/error_description만 있고 토큰은 없다 — 원인 추적용으로만 남긴다
            log.warn("구글 코드 교환 거부 {}: {}", response.statusCode(), response.body())
            throw InvalidGoogleTokenException()
        }
        return objectMapper.readTree(response.body()).path("id_token").asText(null)
            ?: throw InvalidGoogleTokenException()
    }

    companion object {
        private const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
    }
}
