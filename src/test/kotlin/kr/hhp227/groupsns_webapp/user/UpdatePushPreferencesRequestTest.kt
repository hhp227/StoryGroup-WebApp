package kr.hhp227.groupsns_webapp.user

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kr.hhp227.groupsns_webapp.user.dto.UpdatePushPreferencesRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import javax.validation.Validation

// 원시 Boolean이면 누락 필드가 false로 조용히 채워지던 함정(최종 리뷰 지적) — nullable+@NotNull 조합이 400으로 거르는지 고정
class UpdatePushPreferencesRequestTest {
    private val mapper = jacksonObjectMapper()
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `누락 필드는 false가 아니라 null로 역직렬화된다`() {
        val request = mapper.readValue<UpdatePushPreferencesRequest>("""{"chatEnabled":true}""")
        assertEquals(true, request.chatEnabled)
        assertNull(request.activityEnabled)
    }

    @Test
    fun `null 필드는 @NotNull 검증에 걸린다`() {
        val violations = validator.validate(UpdatePushPreferencesRequest(chatEnabled = true, activityEnabled = null))
        assertEquals(1, violations.size)
        assertEquals("activityEnabled", violations.first().propertyPath.toString())
    }

    @Test
    fun `두 필드가 다 있으면 검증 통과`() {
        val violations = validator.validate(UpdatePushPreferencesRequest(chatEnabled = false, activityEnabled = true))
        assertTrue(violations.isEmpty())
    }
}
