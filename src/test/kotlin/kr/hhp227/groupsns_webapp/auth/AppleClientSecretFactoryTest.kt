package kr.hhp227.groupsns_webapp.auth

import io.jsonwebtoken.Jwts
import kr.hhp227.groupsns_webapp.auth.apple.AppleClientSecretFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.util.Base64

// .p8과 같은 PKCS#8 EC(P-256) 키를 즉석 생성해 서명·클레임을 공개키로 되읽는다
class AppleClientSecretFactoryTest {
    private val keyPair = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
    private val pem = "-----BEGIN PRIVATE KEY-----\n" +
        Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(keyPair.private.encoded) +
        "\n-----END PRIVATE KEY-----"

    @Test
    fun `ES256으로 서명하고 kid·iss·sub·aud를 애플 규격대로 넣는다`() {
        val secret = AppleClientSecretFactory("TEAM123", "KEY456", pem).create("com.hhp227.storygroup.signin")
        val jws = Jwts.parserBuilder().setSigningKey(keyPair.public).build().parseClaimsJws(secret)
        assertEquals("KEY456", jws.header.keyId)
        assertEquals("ES256", jws.header.algorithm)
        assertEquals("TEAM123", jws.body.issuer)
        assertEquals("com.hhp227.storygroup.signin", jws.body.subject)
        assertEquals("https://appleid.apple.com", jws.body.audience)
        assertTrue(jws.body.expiration.time - jws.body.issuedAt.time <= 300_000)
    }

    @Test
    fun `env로 들어온 이스케이프 개행(backslash-n)도 파싱한다`() {
        val escaped = pem.replace("\n", "\\n")
        val secret = AppleClientSecretFactory("T", "K", escaped).create("cid")
        assertEquals("cid", Jwts.parserBuilder().setSigningKey(keyPair.public).build().parseClaimsJws(secret).body.subject)
    }
}
