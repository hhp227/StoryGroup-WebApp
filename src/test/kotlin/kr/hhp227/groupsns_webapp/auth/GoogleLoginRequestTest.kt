package kr.hhp227.groupsns_webapp.auth

import kr.hhp227.groupsns_webapp.auth.dto.GoogleCodeLoginRequest
import kr.hhp227.groupsns_webapp.auth.dto.GoogleLoginRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import javax.validation.Validation

// 컨트롤러 @Valid가 쓰는 제약을 Validator로 직접 검증(UpdatePushPreferencesRequestTest 관례)
class GoogleLoginRequestTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `idToken이 비면 위반`() {
        assertEquals(1, validator.validate(GoogleLoginRequest("")).size)
    }

    @Test
    fun `루프백 redirectUri만 허용`() {
        assertTrue(validator.validate(GoogleCodeLoginRequest("c", "v", "http://127.0.0.1:53682")).isEmpty())
        assertTrue(validator.validate(GoogleCodeLoginRequest("c", "v", "http://127.0.0.1:53682/callback")).isEmpty())
        assertEquals(1, validator.validate(GoogleCodeLoginRequest("c", "v", "https://evil.example.com")).size)
        assertEquals(1, validator.validate(GoogleCodeLoginRequest("c", "v", "http://localhost:53682")).size)
        assertEquals(1, validator.validate(GoogleCodeLoginRequest("c", "v", "http://127.0.0.1:53682.evil.com")).size)
    }
}
