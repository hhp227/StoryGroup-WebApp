package kr.hhp227.groupsns_webapp.auth.apple

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

// 애플 토큰(교환)·폐기 엔드포인트. 둘 다 client_secret JWT가 필요해 설정이 없으면 교환은 null, 폐기는 예외
@Component
class AppleHttpTokenClient(
    private val properties: AppleOAuthProperties,
    private val objectMapper: ObjectMapper
) : AppleCodeExchanger, AppleTokenRevoker {
    private val log = LoggerFactory.getLogger(AppleHttpTokenClient::class.java)
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()
    private val secretFactory: AppleClientSecretFactory? by lazy {
        if (properties.isConfigured) AppleClientSecretFactory(properties.teamId, properties.keyId, properties.privateKey) else null
    }

    override fun exchange(code: String, clientId: String, redirectUri: String?): String? {
        val factory = secretFactory ?: return null
        val form = buildMap {
            put("grant_type", "authorization_code")
            put("code", code)
            put("client_id", clientId)
            put("client_secret", factory.create(clientId))
            redirectUri?.let { put("redirect_uri", it) }
        }
        return try {
            val response = post(TOKEN_ENDPOINT, form)
            if (response.statusCode() != 200) {
                log.warn("애플 코드 교환 거부 {}: {}", response.statusCode(), response.body())
                null
            } else {
                objectMapper.readTree(response.body()).path("refresh_token").asText(null)
            }
        } catch (e: Exception) {
            log.warn("애플 코드 교환 요청 실패: {}", e.message)
            null
        }
    }

    override fun revoke(refreshToken: String, clientId: String) {
        val factory = secretFactory ?: throw IllegalStateException("애플 키 미설정으로 폐기 불가")
        val response = post(
            REVOKE_ENDPOINT,
            mapOf(
                "client_id" to clientId,
                "client_secret" to factory.create(clientId),
                "token" to refreshToken,
                "token_type_hint" to "refresh_token"
            )
        )
        check(response.statusCode() == 200) { "애플 토큰 폐기 거부 ${response.statusCode()}: ${response.body()}" }
    }

    private fun post(url: String, form: Map<String, String>): HttpResponse<String> {
        val body = form.entries.joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, Charsets.UTF_8)}" }
        val request = HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(10))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        return http.send(request, HttpResponse.BodyHandlers.ofString())
    }

    private companion object {
        const val TOKEN_ENDPOINT = "https://appleid.apple.com/auth/token"
        const val REVOKE_ENDPOINT = "https://appleid.apple.com/auth/revoke"
    }
}
