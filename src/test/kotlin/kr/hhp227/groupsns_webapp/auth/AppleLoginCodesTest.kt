package kr.hhp227.groupsns_webapp.auth

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import kr.hhp227.groupsns_webapp.auth.apple.AppleLoginCodes
import kr.hhp227.groupsns_webapp.common.exception.InvalidAppleTokenException
import kr.hhp227.groupsns_webapp.security.JwtTokenProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class AppleLoginCodesTest {
    private val secret = "test-secret-test-secret-test-secret-1234"
    private val codes = AppleLoginCodes(secret, 60_000)

    @Test
    fun `verifier가 맞으면 userId를 돌려준다`() {
        val code = codes.issue(7, AppleLoginCodes.hashVerifier("verifier-1"))
        assertEquals(7L, codes.redeem(code, "verifier-1"))
    }

    @Test
    fun `verifier 불일치·만료·위조는 401`() {
        val code = codes.issue(7, AppleLoginCodes.hashVerifier("verifier-1"))
        assertThrows(InvalidAppleTokenException::class.java) { codes.redeem(code, "other") }

        val expired = AppleLoginCodes(secret, -1_000).issue(7, AppleLoginCodes.hashVerifier("v"))
        assertThrows(InvalidAppleTokenException::class.java) { codes.redeem(expired, "v") }

        assertThrows(InvalidAppleTokenException::class.java) { codes.redeem("garbage", "v") }
    }

    @Test
    fun `액세스 토큰 키로 서명한 JWT는 코드로 못 쓰고, 코드는 액세스 토큰으로 못 쓴다`() {
        val forged = Jwts.builder().setSubject("7").claim("typ", "apple_login").claim("vh", AppleLoginCodes.hashVerifier("v"))
            .signWith(Keys.hmacShaKeyFor(secret.toByteArray()), SignatureAlgorithm.HS256).compact()
        assertThrows(InvalidAppleTokenException::class.java) { codes.redeem(forged, "v") }

        val code = codes.issue(7, AppleLoginCodes.hashVerifier("v"))
        assertThrows(Exception::class.java) { JwtTokenProvider(secret, 1_800_000).parse(code) }
    }

    @Test
    fun `hashVerifier는 base64url(SHA-256) — RFC 7636 예시와 같다`() {
        assertEquals("E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", AppleLoginCodes.hashVerifier("dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"))
    }
}
