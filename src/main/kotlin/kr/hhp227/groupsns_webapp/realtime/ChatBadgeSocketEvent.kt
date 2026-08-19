package kr.hhp227.groupsns_webapp.realtime

import com.fasterxml.jackson.annotation.JsonIgnore
import java.time.OffsetDateTime

// 개인 큐(/user/queue/notifications)로 나가는 채팅 뱃지 이벤트 — 방 토픽을 구독하지 않은
// 수신자(셸/채팅 허브)가 방별 미읽음 수와 목록 미리보기(마지막 메시지)를 실시간으로 갱신할 수 있게 한다.
// 본문 표시는 여전히 방 입장 시 REST가 담당 — text/attachmentType/createdAt은 허브 한 줄 미리보기 전용이다
// (첨부 전용 메시지는 text가 빈 문자열, 종류는 attachmentType으로 구분).
data class ChatBadgeSocketEvent(
    // 라우팅(convertAndSendToUser)에만 쓰고 페이로드엔 싣지 않는다.
    @get:JsonIgnore val recipientId: Long,
    val chatRoomId: Long,
    val messageId: Long,
    val senderId: Long,
    val text: String? = null,
    val attachmentType: String? = null,
    val createdAt: OffsetDateTime? = null
) {
    val type: String = "CHAT_MESSAGE"
}
