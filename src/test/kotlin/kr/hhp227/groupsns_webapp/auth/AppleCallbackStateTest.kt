package kr.hhp227.groupsns_webapp.auth

import kr.hhp227.groupsns_webapp.auth.apple.AppleCallbackState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.Base64

class AppleCallbackStateTest {
    private fun encode(json: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray())

    @Test
    fun `ANDROID state는 커스텀 스킴으로 복귀`() {
        val state = AppleCallbackState.parse(encode("""{"p":"ANDROID","n":"nn","vh":"hh"}"""))!!
        assertEquals(AppleCallbackState.Platform.ANDROID, state.platform)
        assertEquals("hh", state.verifierHash)
        assertEquals("storygroup://auth/apple?code=a.b-c&nonce=nn", state.redirectUrl(mapOf("code" to "a.b-c")))
    }

    @Test
    fun `DESKTOP state는 루프백 포트로 복귀`() {
        val state = AppleCallbackState.parse(encode("""{"p":"DESKTOP","port":51234,"n":"nn","vh":"hh"}"""))!!
        assertEquals("http://127.0.0.1:51234/?error=user_cancelled_authorize&nonce=nn", state.redirectUrl(mapOf("error" to "user_cancelled_authorize")))
    }

    @Test
    fun `깨진 base64·필드 누락·포트 범위 위반·DESKTOP 포트 없음은 null`() {
        assertNull(AppleCallbackState.parse("%%%"))
        assertNull(AppleCallbackState.parse(encode("""{"p":"ANDROID","n":"nn"}""")))
        assertNull(AppleCallbackState.parse(encode("""{"p":"DESKTOP","port":80,"n":"nn","vh":"hh"}""")))
        assertNull(AppleCallbackState.parse(encode("""{"p":"DESKTOP","n":"nn","vh":"hh"}""")))
        assertNull(AppleCallbackState.parse(encode("""{"p":"IOS","n":"nn","vh":"hh"}""")))
    }
}
