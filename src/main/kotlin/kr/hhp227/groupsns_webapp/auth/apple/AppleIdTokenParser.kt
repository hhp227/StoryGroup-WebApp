package kr.hhp227.groupsns_webapp.auth.apple

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwsHeader
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SigningKeyResolverAdapter
import kr.hhp227.groupsns_webapp.common.exception.InvalidAppleTokenException
import java.security.Key
import java.security.PublicKey

// 서명(kid별 공개키)·iss·exp·aud 검증. 키 조회를 주입받아 네트워크 없이 테스트한다
class AppleIdTokenParser(
    private val allowedAudiences: List<String>,
    private val keyLookup: (kid: String) -> PublicKey?
) {
    fun parse(idToken: String): AppleIdentity {
        val claims = try {
            Jwts.parserBuilder()
                .requireIssuer(ISSUER)
                .setAllowedClockSkewSeconds(60)
                .setSigningKeyResolver(object : SigningKeyResolverAdapter() {
                    override fun resolveSigningKey(header: JwsHeader<*>, claims: Claims): Key {
                        val kid = header.keyId ?: throw JwtException("kid 없음")
                        return keyLookup(kid) ?: throw JwtException("알 수 없는 kid")
                    }
                })
                .build()
                .parseClaimsJws(idToken)
                .body
        } catch (e: Exception) {
            // 형식·서명·만료·iss·키 조회 실패 전부 인증 실패
            throw InvalidAppleTokenException()
        }
        val audience = claims.audience
        if (audience == null || audience !in allowedAudiences) throw InvalidAppleTokenException()
        val email = claims["email"] as? String
        return AppleIdentity(
            sub = claims.subject ?: throw InvalidAppleTokenException(),
            email = email,
            emailVerified = claims.flag("email_verified"),
            isPrivateEmail = claims.flag("is_private_email") || email?.endsWith("@privaterelay.appleid.com") == true,
            audience = audience
        )
    }

    // 애플은 불리언 클레임을 "true" 문자열로 줄 때가 있다
    private fun Claims.flag(name: String): Boolean = when (val value = get(name)) {
        is Boolean -> value
        is String -> value.equals("true", ignoreCase = true)
        else -> false
    }

    companion object {
        const val ISSUER = "https://appleid.apple.com"
    }
}
