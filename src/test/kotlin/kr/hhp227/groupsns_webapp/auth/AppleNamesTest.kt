package kr.hhp227.groupsns_webapp.auth

import kr.hhp227.groupsns_webapp.auth.apple.AppleNames
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AppleNamesTest {
    @Test
    fun `한글 이름은 성+이름 공백 없이`() {
        assertEquals("홍길동", AppleNames.displayName("길동", "홍", "a@b.com", false))
    }

    @Test
    fun `영문 이름은 이름 성`() {
        assertEquals("John Smith", AppleNames.displayName("John", "Smith", "a@b.com", false))
    }

    @Test
    fun `한쪽만 있으면 그 값`() {
        assertEquals("길동", AppleNames.displayName(" 길동 ", null, "a@b.com", false))
    }

    @Test
    fun `이름 없고 relay면 애플 사용자, 일반 이메일이면 null(이메일 앞부분)`() {
        assertEquals("애플 사용자", AppleNames.displayName(null, "", "x@privaterelay.appleid.com", true))
        assertEquals("애플 사용자", AppleNames.displayName(null, null, null, false))
        assertNull(AppleNames.displayName(null, null, "me@icloud.com", false))
    }

    @Test
    fun `콜백 user JSON에서 이름을 뽑고 깨진 JSON은 무시`() {
        assertEquals("길동" to "홍", AppleNames.fromUserJson("""{"name":{"firstName":"길동","lastName":"홍"},"email":"a@b.com"}"""))
        assertEquals(null to null, AppleNames.fromUserJson(null))
        assertEquals(null to null, AppleNames.fromUserJson("{broken"))
    }
}
