package kr.hhp227.groupsns_webapp.security

import io.jsonwebtoken.ExpiredJwtException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

// DB나 Spring 컨텍스트 없이 순수 JWT 서명/파싱 로직만 검증(스프링 부트 통합 테스트는 라이브 DB 필요).
class JwtTokenProviderTests {
    private val secret = "test-only-secret-key-at-least-32-bytes-long-1234567890"

    @Test
    fun `발급한 토큰에서 원래 userId를 그대로 복원한다`() {
        val provider = JwtTokenProvider(secret, 1_800_000)
        val token = provider.generateAccessToken(userId = 42L, email = "user@example.com")

        assertEquals(42L, provider.getUserId(token))
    }

    @Test
    fun `만료된 토큰은 파싱 시 예외를 던진다`() {
        val provider = JwtTokenProvider(secret, accessTokenExpirationMs = -1_000)
        val expiredToken = provider.generateAccessToken(userId = 1L, email = "user@example.com")

        assertThrows(ExpiredJwtException::class.java) {
            provider.getUserId(expiredToken)
        }
    }

    @Test
    fun `다른 시크릿으로 서명된 토큰은 검증에 실패한다`() {
        val provider = JwtTokenProvider(secret, 1_800_000)
        val otherProvider = JwtTokenProvider("different-secret-key-at-least-32-bytes-long-abcdefg", 1_800_000)
        val token = provider.generateAccessToken(userId = 1L, email = "user@example.com")

        assertThrows(io.jsonwebtoken.security.SignatureException::class.java) {
            otherProvider.getUserId(token)
        }
    }
}
