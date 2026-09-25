package kr.hhp227.groupsns_webapp.auth

import com.fasterxml.jackson.databind.ObjectMapper
import kr.hhp227.groupsns_webapp.auth.google.GoogleHttpAccessTokenVerifier.Companion.toIdentity
import kr.hhp227.groupsns_webapp.common.exception.InvalidGoogleTokenException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// tokeninfo/userinfo 응답 → GoogleIdentity 변환(네트워크 제외). 응답 모양은 구글 문서 예시 기준
class GoogleHttpAccessTokenVerifierTest {
    private val json = ObjectMapper()
    private val allowed = listOf("web-client")

    private fun tokenInfo(aud: String = "web-client", emailVerified: String = "true") = json.readTree(
        """{"azp":"$aud","aud":"$aud","sub":"g-1","scope":"openid email profile","expires_in":"3500","email":"a@gmail.com","email_verified":"$emailVerified"}"""
    )

    @Test
    fun `aud가 허용 목록이면 tokeninfo와 userinfo를 합친다`() {
        val identity = toIdentity(tokenInfo(), json.readTree("""{"sub":"g-1","name":"구글이","picture":"https://pic"}"""), allowed)
        assertEquals("g-1", identity.sub)
        assertEquals("a@gmail.com", identity.email)
        assertTrue(identity.emailVerified)
        assertEquals("구글이", identity.name)
        assertEquals("https://pic", identity.picture)
    }

    @Test
    fun `다른 앱이 받은 토큰(aud 불일치)은 거부`() {
        assertThrows(InvalidGoogleTokenException::class.java) { toIdentity(tokenInfo(aud = "evil-client"), null, allowed) }
    }

    @Test
    fun `userinfo 실패여도 로그인은 진행하고 이름은 비운다`() {
        val identity = toIdentity(tokenInfo(emailVerified = "false"), null, allowed)
        assertNull(identity.name)
        assertEquals(false, identity.emailVerified)
    }

    @Test
    fun `email 범위가 없으면 거부`() {
        val noEmail = json.readTree("""{"aud":"web-client","sub":"g-1"}""")
        assertThrows(InvalidGoogleTokenException::class.java) { toIdentity(noEmail, null, allowed) }
    }
}
