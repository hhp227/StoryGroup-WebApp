package kr.hhp227.groupsns_webapp.realtime

import kr.hhp227.groupsns_webapp.security.UserPrincipal
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Controller
import java.security.Principal

// Typing은 DB를 안 거치는 휘발성 신호(설계 문서 §7) — REST 없이 STOMP SEND로 받아
// 같은 방 토픽에 TYPING 이벤트로 즉시 릴레이한다. 방 접근권은
// StompAuthChannelInterceptor의 SEND 검사가 이 핸들러 진입 전에 이미 보장한다.
@Controller
class TypingController(private val broadcaster: ChatEventBroadcaster) {

    @MessageMapping("/chat-rooms/{chatRoomId}/typing")
    fun typing(@DestinationVariable chatRoomId: Long, principal: Principal) {
        val user = (principal as UsernamePasswordAuthenticationToken).principal as UserPrincipal
        broadcaster.relay(ChatSocketEvent.typing(chatRoomId, user.id, user.name))
    }
}
