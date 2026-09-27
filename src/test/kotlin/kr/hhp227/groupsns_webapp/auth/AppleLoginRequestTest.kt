package kr.hhp227.groupsns_webapp.auth

import kr.hhp227.groupsns_webapp.auth.dto.AppleLoginRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import javax.validation.Validation

class AppleLoginRequestTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `clientType은 IOS·WEB만`() {
        assertTrue(validator.validate(AppleLoginRequest("t", "c", "IOS")).isEmpty())
        assertTrue(validator.validate(AppleLoginRequest("t", null, "WEB")).isEmpty())
        assertEquals(1, validator.validate(AppleLoginRequest("t", "c", "ANDROID")).size)
        assertEquals(1, validator.validate(AppleLoginRequest("", "c", "IOS")).size)
    }

    @Test
    fun `복귀 HTML은 URL을 이스케이프해 링크와 자동 이동에 싣는다`() {
        val html = AuthController.appleReturnHtml("storygroup://auth/apple?code=a&nonce=\"x<")
        assertTrue(html.contains("href=\"storygroup://auth/apple?code=a&amp;nonce=&quot;x&lt;\""))
        assertTrue(html.contains("location.replace"))
    }
}
