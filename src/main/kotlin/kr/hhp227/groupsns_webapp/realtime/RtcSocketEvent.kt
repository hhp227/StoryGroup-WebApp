package kr.hhp227.groupsns_webapp.realtime

// WebRTC 시그널링 이벤트(Phase 7 설계 문서). 전부 휘발성 — DB를 거치지 않는다.
// roomKey는 그룹 회의("meetings/{meetingId}")와 DM 통화("chat-rooms/{chatRoomId}")를
// 같은 코드 경로로 다루기 위한 식별자(D2).

enum class RtcSignalType { OFFER, ANSWER, ICE }

data class RtcPeer(val userId: Long, val userName: String)

// /topic/rtc/{roomKey}로 나가는 로스터 이벤트 — 증분이 아니라 항상 전체 목록(수신 측 자가 복구, D3).
data class RtcPeersEvent(
    val roomKey: String,
    val peers: List<RtcPeer>
) {
    val type: String = "PEERS"
}

// 클라이언트가 /app/rtc/{...}/signal로 SEND하는 body. payload는 SDP/ICE JSON을 담은
// 불투명 문자열 — 서버는 파싱하지 않고 그대로 릴레이한다(D4).
data class RtcSignalRequest(
    val type: RtcSignalType,
    val toUserId: Long,
    val payload: String
)

// /user/queue/rtc로 표적 전달되는 릴레이 envelope.
data class RtcSignalEvent(
    val type: RtcSignalType,
    val roomKey: String,
    val fromUserId: Long,
    val fromUserName: String,
    val payload: String
)

// DM 통화 벨울림(D6) — 상대의 개인 알림 큐(/user/queue/notifications)로 나간다.
// NotificationSocketEvent와 같은 큐를 타므로 type 필드로 구분된다(그쪽은 "NOTIFICATION").
data class CallInviteEvent(
    val chatRoomId: Long,
    val fromUserId: Long,
    val fromUserName: String
) {
    val type: String = "CALL_INVITE"
}
