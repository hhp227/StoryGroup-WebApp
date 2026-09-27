package kr.hhp227.groupsns_webapp.auth.apple

import com.fasterxml.jackson.databind.ObjectMapper

// 애플은 이름을 최초 인가 1회만 준다(iOS fullName·웹 user.name·콜백 user JSON) — 설계 §2.3 이름 규칙
object AppleNames {
    private const val FALLBACK = "애플 사용자"
    private val HANGUL = Regex("[가-힣]")
    private val json = ObjectMapper()

    /** null이면 호출부(resolveOauthUser)가 이메일 앞부분을 쓴다 */
    fun displayName(firstName: String?, lastName: String?, email: String?, isPrivateEmail: Boolean): String? {
        val first = firstName?.trim().orEmpty()
        val last = lastName?.trim().orEmpty()
        val full = when {
            first.isEmpty() -> last
            last.isEmpty() -> first
            HANGUL.containsMatchIn(first + last) -> last + first
            else -> "$first $last"
        }
        if (full.isNotEmpty()) return full
        // relay 주소 앞부분은 무작위 문자열이라 이름으로 쓰지 않는다
        return if (isPrivateEmail || email == null) FALLBACK else null
    }

    /** 콜백 form의 user 파라미터 — {"name":{"firstName","lastName"},"email"} */
    fun fromUserJson(userJson: String?): Pair<String?, String?> {
        if (userJson.isNullOrBlank()) return null to null
        val name = runCatching { json.readTree(userJson).path("name") }.getOrNull() ?: return null to null
        return name.path("firstName").asText(null) to name.path("lastName").asText(null)
    }
}
