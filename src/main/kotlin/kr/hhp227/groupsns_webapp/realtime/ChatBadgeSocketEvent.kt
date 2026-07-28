package kr.hhp227.groupsns_webapp.realtime

import com.fasterxml.jackson.annotation.JsonIgnore

// 개인 큐(/user/queue/notifications)로 나가는 채팅 뱃지 이벤트 — 방 토픽을 구독하지 않은
// 수신자(셸/채팅 허브)가 방별 미읽음 수를 실시간으로 올릴 수 있게 한다.
// 메시지 본문은 싣지 않는다: 내용은 방 입장 시 REST로 읽고, 뱃지에는 "어느 방에 새 메시지" 사실만 필요.
data class ChatBadgeSocketEvent(
    // 라우팅(convertAndSendToUser)에만 쓰고 페이로드엔 싣지 않는다.
    @get:JsonIgnore val recipientId: Long,
    val chatRoomId: Long,
    val messageId: Long,
    val senderId: Long
) {
    val type: String = "CHAT_MESSAGE"
}
