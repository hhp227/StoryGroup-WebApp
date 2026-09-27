package kr.hhp227.groupsns_webapp.auth.apple

import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URLEncoder
import java.util.Base64

// 앱이 만든 state(설계 §2.4) — 복귀 대상(딥링크/루프백 포트)과 교환 때 확인할 verifier 해시를 싣는다
data class AppleCallbackState(
    val platform: Platform,
    val port: Int?,
    val nonce: String,
    val verifierHash: String
) {
    enum class Platform { ANDROID, DESKTOP }

    // nonce는 앱이 자기 요청의 응답인지 확인하는 데 쓴다
    fun redirectUrl(params: Map<String, String>): String {
        val query = (params + ("nonce" to nonce)).entries
            .joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, Charsets.UTF_8)}" }
        return when (platform) {
            Platform.ANDROID -> "storygroup://auth/apple?$query"
            Platform.DESKTOP -> "http://127.0.0.1:$port/?$query"
        }
    }

    companion object {
        private val json = ObjectMapper()

        /** 형식 위반이면 null — 복귀 대상을 알 수 없으니 호출부가 400 HTML로 끝낸다 */
        fun parse(raw: String): AppleCallbackState? = runCatching {
            val node = json.readTree(Base64.getUrlDecoder().decode(raw))
            val platform = Platform.valueOf(node.path("p").asText())
            val nonce = node.path("n").asText("").takeIf { it.isNotEmpty() } ?: return null
            val hash = node.path("vh").asText("").takeIf { it.isNotEmpty() } ?: return null
            val port = if (node.has("port")) node.path("port").asInt() else null
            if (platform == Platform.DESKTOP && (port == null || port !in 1024..65535)) return null
            AppleCallbackState(platform, if (platform == Platform.DESKTOP) port else null, nonce, hash)
        }.getOrNull()
    }
}
