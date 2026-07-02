package kr.hhp227.groupsns_webapp.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.access-token-expiration-ms}") private val accessTokenExpirationMs: Long
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray(Charsets.UTF_8))

    fun generateAccessToken(userId: Long, email: String): String {
        val now = Date()
        val expiry = Date(now.time + accessTokenExpirationMs)
        return Jwts.builder()
            .setSubject(userId.toString())
            .claim("email", email)
            .setIssuedAt(now)
            .setExpiration(expiry)
            .signWith(key, SignatureAlgorithm.HS256)
            .compact()
    }

    fun accessTokenExpirationSeconds(): Long = accessTokenExpirationMs / 1000

    fun parse(token: String): Jws<Claims> =
        Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token)

    fun getUserId(token: String): Long = parse(token).body.subject.toLong()
}
