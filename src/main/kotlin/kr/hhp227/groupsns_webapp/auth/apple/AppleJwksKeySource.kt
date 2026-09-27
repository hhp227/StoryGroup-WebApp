package kr.hhp227.groupsns_webapp.auth.apple

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.RSAPublicKeySpec
import java.time.Duration
import java.util.Base64

// 애플 공개키(JWKS) 캐시 — kid 미스일 때만 재조회(키 회전 대응), 재조회는 1분에 한 번으로 제한
@Component
class AppleJwksKeySource(private val objectMapper: ObjectMapper) {
    private val log = LoggerFactory.getLogger(AppleJwksKeySource::class.java)
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()

    @Volatile private var keys: Map<String, PublicKey> = emptyMap()
    @Volatile private var lastFetchMillis = 0L

    fun find(kid: String): PublicKey? {
        keys[kid]?.let { return it }
        synchronized(this) {
            keys[kid]?.let { return it }
            if (System.currentTimeMillis() - lastFetchMillis < 60_000) return null
            lastFetchMillis = System.currentTimeMillis()
            keys = runCatching { fetch() }.onFailure { log.warn("애플 JWKS 조회 실패: {}", it.message) }.getOrDefault(keys)
            return keys[kid]
        }
    }

    private fun fetch(): Map<String, PublicKey> {
        val request = HttpRequest.newBuilder(URI.create(JWKS_URL)).timeout(Duration.ofSeconds(10)).GET().build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        check(response.statusCode() == 200) { "JWKS ${response.statusCode()}" }
        return parseJwks(objectMapper.readTree(response.body()))
    }

    companion object {
        private const val JWKS_URL = "https://appleid.apple.com/auth/keys"

        fun parseJwks(json: JsonNode): Map<String, PublicKey> {
            val decoder = Base64.getUrlDecoder()
            val factory = KeyFactory.getInstance("RSA")
            return json.path("keys").filter { it.path("kty").asText() == "RSA" }.associate { key ->
                val modulus = BigInteger(1, decoder.decode(key.path("n").asText()))
                val exponent = BigInteger(1, decoder.decode(key.path("e").asText()))
                key.path("kid").asText() to factory.generatePublic(RSAPublicKeySpec(modulus, exponent))
            }
        }
    }
}
