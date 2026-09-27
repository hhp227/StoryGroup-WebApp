package kr.hhp227.groupsns_webapp.auth

import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import kr.hhp227.groupsns_webapp.auth.apple.AppleIdTokenParser
import kr.hhp227.groupsns_webapp.auth.apple.AppleJwksKeySource
import kr.hhp227.groupsns_webapp.common.exception.InvalidAppleTokenException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import java.util.Date

class AppleIdTokenParserTest {
    private val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val otherKey = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val parser = AppleIdTokenParser(listOf("com.hhp227.Application", "svc")) { kid ->
        if (kid == "k1") keyPair.public else null
    }

    private fun token(
        aud: String = "com.hhp227.Application",
        iss: String = "https://appleid.apple.com",
        kid: String = "k1",
        exp: Date = Date(System.currentTimeMillis() + 600_000),
        claims: Map<String, Any> = mapOf("email" to "a@privaterelay.appleid.com", "email_verified" to "true", "is_private_email" to "true"),
        signer: java.security.PrivateKey = keyPair.private
    ): String = Jwts.builder()
        .setHeaderParam("kid", kid)
        .setIssuer(iss).setAudience(aud).setSubject("apple-sub-1").setExpiration(exp)
        .addClaims(claims)
        .signWith(signer, SignatureAlgorithm.RS256)
        .compact()

    @Test
    fun `정상 토큰은 sub·email·문자열 불리언·aud를 뽑는다`() {
        val identity = parser.parse(token())
        assertEquals("apple-sub-1", identity.sub)
        assertEquals("a@privaterelay.appleid.com", identity.email)
        assertTrue(identity.emailVerified)
        assertTrue(identity.isPrivateEmail)
        assertEquals("com.hhp227.Application", identity.audience)
    }

    @Test
    fun `불리언 클레임과 email 없음도 받는다`() {
        val identity = parser.parse(token(aud = "svc", claims = mapOf("email_verified" to true)))
        assertNull(identity.email)
        assertTrue(identity.emailVerified)
        assertFalse(identity.isPrivateEmail)
        assertEquals("svc", identity.audience)
    }

    @Test
    fun `aud·iss·만료·kid·서명 위반은 전부 401 예외`() {
        assertThrows(InvalidAppleTokenException::class.java) { parser.parse(token(aud = "evil")) }
        assertThrows(InvalidAppleTokenException::class.java) { parser.parse(token(iss = "https://evil")) }
        assertThrows(InvalidAppleTokenException::class.java) { parser.parse(token(exp = Date(System.currentTimeMillis() - 600_000))) }
        assertThrows(InvalidAppleTokenException::class.java) { parser.parse(token(kid = "unknown")) }
        assertThrows(InvalidAppleTokenException::class.java) { parser.parse(token(signer = otherKey.private)) }
        assertThrows(InvalidAppleTokenException::class.java) { parser.parse("not-a-jwt") }
    }

    @Test
    fun `JWKS 응답의 n·e로 RSA 공개키를 만든다`() {
        val pub = keyPair.public as RSAPublicKey
        val enc = Base64.getUrlEncoder().withoutPadding()
        fun unsigned(b: java.math.BigInteger) = b.toByteArray().let { if (it[0] == 0.toByte()) it.copyOfRange(1, it.size) else it }
        val json = ObjectMapper().readTree(
            """{"keys":[{"kty":"RSA","kid":"k1","use":"sig","alg":"RS256","n":"${enc.encodeToString(unsigned(pub.modulus))}","e":"${enc.encodeToString(unsigned(pub.publicExponent))}"}]}"""
        )
        val keys = AppleJwksKeySource.parseJwks(json)
        assertEquals(pub, keys["k1"])
    }
}
