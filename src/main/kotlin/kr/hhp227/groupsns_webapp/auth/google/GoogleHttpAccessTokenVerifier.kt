package kr.hhp227.groupsns_webapp.auth.google

import com.fasterxml.jackson.databind.JsonNode
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

/**
 * 액세스 토큰은 서명 검증이 안 되는 불투명 토큰이라 구글 tokeninfo로 발급 대상(aud)·만료를 확인한다.
 * aud 확인이 핵심 — 남의 앱이 받은 토큰을 들고 와 우리 계정으로 로그인하는 토큰 치환을 막는다.
 * 이름·사진은 tokeninfo에 없어 userinfo에서 보충한다(실패해도 로그인은 진행 — 신규 가입 시 이메일 앞부분 이름).
 */
@Component
class GoogleHttpAccessTokenVerifier(
    private val properties: GoogleOAuthProperties,
    private val objectMapper: ObjectMapper
) : GoogleAccessTokenVerifier {
    private val log = LoggerFactory.getLogger(GoogleHttpAccessTokenVerifier::class.java)
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()

    override fun verify(accessToken: String): GoogleIdentity {
        val tokenInfo = get("$TOKENINFO_ENDPOINT?access_token=${URLEncoder.encode(accessToken, Charsets.UTF_8)}", null)
            ?: throw InvalidGoogleTokenException()
        val userInfo = get(USERINFO_ENDPOINT, accessToken)
        return toIdentity(tokenInfo, userInfo, properties.clientIds)
    }

    // 200이 아니거나 네트워크 실패면 null — 호출부가 필수/선택 여부로 처리
    private fun get(url: String, bearer: String?): JsonNode? {
        val builder = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(10)).GET()
        bearer?.let { builder.header("Authorization", "Bearer $it") }
        return try {
            val response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) objectMapper.readTree(response.body()) else {
                log.warn("구글 토큰 조회 거부 {} {}", response.statusCode(), url.substringBefore('?'))
                null
            }
        } catch (e: Exception) {
            log.warn("구글 토큰 조회 실패 {}: {}", url.substringBefore('?'), e.message)
            null
        }
    }

    companion object {
        private const val TOKENINFO_ENDPOINT = "https://oauth2.googleapis.com/tokeninfo"
        private const val USERINFO_ENDPOINT = "https://openidconnect.googleapis.com/v1/userinfo"

        // 네트워크와 분리한 순수 변환 — 단위 테스트 대상. email_verified는 tokeninfo가 문자열 "true"로 준다
        fun toIdentity(tokenInfo: JsonNode, userInfo: JsonNode?, allowedAudiences: List<String>): GoogleIdentity {
            val aud = tokenInfo.path("aud").asText("")
            if (aud !in allowedAudiences) throw InvalidGoogleTokenException()
            val sub = tokenInfo.path("sub").asText("").ifEmpty { throw InvalidGoogleTokenException() }
            val email = tokenInfo.path("email").asText("").ifEmpty { throw InvalidGoogleTokenException() }
            return GoogleIdentity(
                sub = sub,
                email = email,
                emailVerified = tokenInfo.path("email_verified").asText() == "true",
                name = userInfo?.path("name")?.asText(null),
                picture = userInfo?.path("picture")?.asText(null)
            )
        }
    }
}
