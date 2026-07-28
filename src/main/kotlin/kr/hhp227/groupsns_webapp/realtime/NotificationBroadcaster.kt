package kr.hhp227.groupsns_webapp.realtime

import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

// 알림 행이 커밋된 뒤에만 수신자의 개인 큐로 내보낸다(롤백된 알림이 전달되는 일 방지).
// 사용자 라우팅 키는 STOMP 세션 Principal.name — StompAuthChannelInterceptor가 userId 문자열로 세팅한다.
@Component
class NotificationBroadcaster(private val messagingTemplate: SimpMessagingTemplate) {

    @TransactionalEventListener
    fun on(event: NotificationSocketEvent) {
        messagingTemplate.convertAndSendToUser(event.recipientId.toString(), "/queue/notifications", event)
    }

    // 채팅 뱃지도 같은 개인 큐로 — 클라이언트는 envelope의 type(NOTIFICATION/CHAT_MESSAGE/CALL_INVITE)으로 분기한다.
    @TransactionalEventListener
    fun on(event: ChatBadgeSocketEvent) {
        messagingTemplate.convertAndSendToUser(event.recipientId.toString(), "/queue/notifications", event)
    }
}
