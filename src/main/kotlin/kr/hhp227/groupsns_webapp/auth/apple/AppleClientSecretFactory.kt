package kr.hhp227.groupsns_webapp.auth.apple

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import java.util.Date

// 애플 토큰·폐기 API의 client_secret = .p8(ES256)로 서명한 5분짜리 JWT. 매 호출 새로 만든다
class AppleClientSecretFactory(private val teamId: String, private val keyId: String, privateKeyPem: String) {
    private val key: PrivateKey = parsePkcs8(privateKeyPem)

    fun create(clientId: String): String {
        val now = System.currentTimeMillis()
        return Jwts.builder()
            .setHeaderParam("kid", keyId)
            .setIssuer(teamId)
            .setIssuedAt(Date(now))
            .setExpiration(Date(now + 300_000))
            .setAudience("https://appleid.apple.com")
            .setSubject(clientId)
            .signWith(key, SignatureAlgorithm.ES256)
            .compact()
    }

    private companion object {
        // Secret Manager → env 경로에 따라 개행이 실제 개행이거나 "\n" 문자열로 온다 — 둘 다 받는다
        fun parsePkcs8(pem: String): PrivateKey {
            val body = pem.replace("\\n", "\n").lines()
                .filterNot { it.startsWith("-----") }
                .joinToString("") { it.trim() }
            return KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(body)))
        }
    }
}
