package kr.hhp227.groupsns_webapp.auth.apple

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import kr.hhp227.groupsns_webapp.common.exception.InvalidAppleTokenException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.util.Base64
import java.util.Date
import javax.crypto.SecretKey

// Android·Desktop 콜백 → 앱 복귀용 60초 코드(설계 §2.4). 저장소 없이 서명 JWT — Cloud Run 다중 인스턴스 안전.
// 액세스 토큰과 다른 파생 키로 서명해 JWT 필터가 코드를 액세스 토큰으로 받아들이지 않게 한다.
// verifier는 앱만 가지므로 딥링크를 가로채도 교환할 수 없다
@Component
class AppleLoginCodes(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${app.oauth.apple.login-code-ttl-ms}") private val ttlMillis: Long
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(sha256("apple-login:$secret".toByteArray(Charsets.UTF_8)))

    fun issue(userId: Long, verifierHash: String): String = Jwts.builder()
        .setSubject(userId.toString())
        .claim("typ", TYPE)
        .claim("vh", verifierHash)
        .setExpiration(Date(System.currentTimeMillis() + ttlMillis))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact()

    fun redeem(code: String, verifier: String): Long {
        val claims = try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(code).body
        } catch (e: Exception) {
            throw InvalidAppleTokenException()
        }
        if (claims["typ"] != TYPE) throw InvalidAppleTokenException()
        val expected = (claims["vh"] as? String)?.toByteArray() ?: throw InvalidAppleTokenException()
        if (!MessageDigest.isEqual(expected, hashVerifier(verifier).toByteArray())) throw InvalidAppleTokenException()
        return claims.subject?.toLongOrNull() ?: throw InvalidAppleTokenException()
    }

    companion object {
        private const val TYPE = "apple_login"

        /** 앱의 state.vh와 같은 계산: base64url(SHA-256(verifier ASCII)), 패딩 없음 */
        fun hashVerifier(verifier: String): String =
            Base64.getUrlEncoder().withoutPadding().encodeToString(sha256(verifier.toByteArray(Charsets.US_ASCII)))

        private fun sha256(bytes: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(bytes)
    }
}
