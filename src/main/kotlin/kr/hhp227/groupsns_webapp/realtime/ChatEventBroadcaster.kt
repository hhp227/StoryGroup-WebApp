package kr.hhp227.groupsns_webapp.realtime

import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

// 브로드캐스트 출구를 이 한 곳으로 모은다 — 다중 인스턴스로 확장할 때(Postgres LISTEN/NOTIFY
// 릴레이 등) 이 컴포넌트만 교체하면 되고, 서비스 코드는 ApplicationEvent 발행만 안다(설계 문서 D2).
@Component
class ChatEventBroadcaster(private val messagingTemplate: SimpMessagingTemplate) {

    // 기본 phase가 AFTER_COMMIT — 롤백된 메시지가 방송되는 일이 없다.
    @TransactionalEventListener
    fun on(event: ChatSocketEvent) = relay(event)

    // Typing처럼 DB를 안 거치는 휘발성 이벤트는 트랜잭션이 없어 @TransactionalEventListener가
    // 이벤트를 버리므로, 발행처가 이 메서드를 직접 호출한다.
    fun relay(event: ChatSocketEvent) {
        messagingTemplate.convertAndSend("/topic/chat-rooms/${event.chatRoomId}", event)
    }
}
