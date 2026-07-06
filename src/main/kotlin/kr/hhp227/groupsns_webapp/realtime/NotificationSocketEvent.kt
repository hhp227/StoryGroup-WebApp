package kr.hhp227.groupsns_webapp.realtime

import com.fasterxml.jackson.annotation.JsonIgnore
import kr.hhp227.groupsns_webapp.notification.dto.NotificationResponse

// 개인 큐(/user/queue/notifications)로 나가는 알림 이벤트.
// 채팅 토픽의 ChatSocketEvent처럼 type 필드가 있는 envelope — 나중에 다른 개인 이벤트를 얹을 수 있다.
data class NotificationSocketEvent(
    // 라우팅(convertAndSendToUser)에만 쓰고 페이로드엔 싣지 않는다.
    @get:JsonIgnore val recipientId: Long,
    val notification: NotificationResponse
) {
    val type: String = "NOTIFICATION"
}
