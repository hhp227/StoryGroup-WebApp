package kr.hhp227.groupsns_webapp.realtime

import kr.hhp227.groupsns_webapp.security.UserPrincipal
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// 필드명이 브라우저 RTCIceServer와 동일해서 프론트가 응답을 그대로 RTCPeerConnection에 넣을 수 있다.
data class IceServerResponse(val urls: List<String>, val username: String? = null, val credential: String? = null)
data class IceServersResponse(val iceServers: List<IceServerResponse>)

// WebRTC ICE 서버 구성 조회(Phase 7 D10). 프론트에 ICE 서버를 하드코딩하지 않고 여기서 내려준다 —
// 지금은 STUN만 반환하고, 나중에 TURN 서버를 준비하면 env 두 개(APP_RTC_TURN_URLS/SECRET)만
// 채우면 코드 변경 없이 TURN 항목이 응답에 추가된다.
@RestController
@RequestMapping("/api/rtc")
class RtcIceController(
    @Value("\${app.rtc.stun-urls}") private val stunUrls: String,
    @Value("\${app.rtc.turn-urls}") private val turnUrls: String,
    @Value("\${app.rtc.turn-secret}") private val turnSecret: String,
    @Value("\${app.rtc.turn-credential-ttl-seconds}") private val turnCredentialTtlSeconds: Long
) {
    @GetMapping("/ice-servers")
    fun iceServers(@AuthenticationPrincipal principal: UserPrincipal): IceServersResponse {
        val servers = mutableListOf<IceServerResponse>()
        splitUrls(stunUrls).takeIf { it.isNotEmpty() }?.let { servers.add(IceServerResponse(it)) }
        val turn = splitUrls(turnUrls)
        if (turn.isNotEmpty() && turnSecret.isNotBlank()) {
            // coturn use-auth-secret(TURN REST API) 방식: 자격증명을 저장하지 않고
            // "만료시각:유저ID"를 username으로, HMAC-SHA1(username, 공유 시크릿)을 credential로 쓴다.
            // TURN 서버는 같은 시크릿으로 검증만 하면 되고, 만료가 지나면 자격증명이 저절로 무효화된다.
            val username = "${Instant.now().epochSecond + turnCredentialTtlSeconds}:${principal.id}"
            servers.add(IceServerResponse(turn, username, hmacSha1Base64(username, turnSecret)))
        }
        return IceServersResponse(servers)
    }

    private fun splitUrls(raw: String): List<String> =
        raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    private fun hmacSha1Base64(message: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA1"))
        return Base64.getEncoder().encodeToString(mac.doFinal(message.toByteArray(Charsets.UTF_8)))
    }
}
