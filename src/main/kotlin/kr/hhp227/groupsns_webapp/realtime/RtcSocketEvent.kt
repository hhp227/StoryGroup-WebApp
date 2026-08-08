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

// 클라이언트가 /app/rtc/chat-rooms/{id}/invite로 SEND하는 body(선택).
// 빈 바디(웹·구버전 앱)는 페이스톡(video=true)으로 간주한다 — 하위호환.
data class CallInviteRequest(
    val video: Boolean = true
)

// DM 통화 벨울림(D6) — 상대의 개인 알림 큐(/user/queue/notifications)로 나간다.
// NotificationSocketEvent와 같은 큐를 타므로 type 필드로 구분된다(그쪽은 "NOTIFICATION").
data class CallInviteEvent(
    val chatRoomId: Long,
    val fromUserId: Long,
    val fromUserName: String,
    // 그룹 방 벨울림(페이스톡 전환) — DM이면 null. 수신 측이 배너 제목과 이동 경로(그룹 채팅)를 만든다.
    val groupId: Long? = null,
    val roomName: String? = null,
    // false면 보이스톡 — 수신 측이 배너 표시와 카메라 OFF 입장을 결정한다(모바일 전용, 웹은 항상 true)
    val video: Boolean = true
) {
    val type: String = "CALL_INVITE"
}
