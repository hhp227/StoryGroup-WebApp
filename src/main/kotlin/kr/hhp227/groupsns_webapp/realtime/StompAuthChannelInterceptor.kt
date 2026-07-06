package kr.hhp227.groupsns_webapp.realtime

import io.jsonwebtoken.JwtException
import kr.hhp227.groupsns_webapp.chat.ChatRoomMapper
import kr.hhp227.groupsns_webapp.group.UserGroupMapper
import kr.hhp227.groupsns_webapp.security.JwtTokenProvider
import kr.hhp227.groupsns_webapp.security.UserPrincipal
import kr.hhp227.groupsns_webapp.user.UserMapper
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.messaging.support.MessageHeaderAccessor
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Component

// REST의 JwtAuthenticationFilter에 대응하는 STOMP 쪽 인증/인가.
// 브라우저 WebSocket API는 커스텀 HTTP 헤더를 못 실으므로 handshake(/ws)는 permitAll로 열어두고,
// CONNECT 프레임의 Authorization 네이티브 헤더로 인증한다(설계 문서 D3).
// 여기서 던진 예외는 클라이언트에 ERROR 프레임으로 전달되고 연결이 끊긴다.
@Component
class StompAuthChannelInterceptor(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userMapper: UserMapper,
    private val chatRoomMapper: ChatRoomMapper,
    private val userGroupMapper: UserGroupMapper
) : ChannelInterceptor {

    override fun preSend(message: Message<*>, channel: MessageChannel): Message<*> {
        val accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor::class.java) ?: return message
        when (accessor.command) {
            StompCommand.CONNECT -> authenticate(accessor)
            StompCommand.SUBSCRIBE -> authorizeSubscription(accessor)
            StompCommand.SEND -> authorizeSend(accessor)
            else -> {}
        }
        return message
    }

    private fun authenticate(accessor: StompHeaderAccessor) {
        val header = accessor.getFirstNativeHeader("Authorization")
        if (header == null || !header.startsWith("Bearer ")) throw AccessDeniedException("인증 토큰이 없습니다")
        val userId = try {
            jwtTokenProvider.getUserId(header.removePrefix("Bearer "))
        } catch (ex: JwtException) {
            throw AccessDeniedException("유효하지 않은 토큰입니다")
        } catch (ex: IllegalArgumentException) {
            throw AccessDeniedException("유효하지 않은 토큰입니다")
        }
        val user = userMapper.findById(userId) ?: throw AccessDeniedException("유효하지 않은 토큰입니다")
        val principal = UserPrincipal.from(user)
        // accessor.user에 넣어두면 이후 같은 세션의 모든 프레임(SUBSCRIBE 등)에서 꺼내 쓸 수 있다.
        accessor.user = StompUserToken(principal)
    }

    private fun authorizeSubscription(accessor: StompHeaderAccessor) {
        val principal = requirePrincipal(accessor)
        val destination = accessor.destination ?: throw AccessDeniedException("destination이 없습니다")
        // 개인 알림 큐는 Spring이 세션별로 해석해 본인에게만 전달하므로 인증만으로 충분하다.
        if (destination == USER_NOTIFICATIONS_DESTINATION) return
        val roomId = TOPIC_PATTERN.matchEntire(destination)?.groupValues?.get(1)?.toLongOrNull()
            ?: throw AccessDeniedException("허용되지 않은 destination입니다")
        requireRoomAccess(principal, roomId)
    }

    // SimpleBroker는 클라이언트가 /topic/**으로 직접 SEND한 프레임도 구독자에게 그대로 중계하므로,
    // 화이트리스트(typing destination)에 없는 SEND는 전부 거부해 이벤트 위조를 막는다.
    private fun authorizeSend(accessor: StompHeaderAccessor) {
        val principal = requirePrincipal(accessor)
        val destination = accessor.destination ?: throw AccessDeniedException("destination이 없습니다")
        val roomId = TYPING_PATTERN.matchEntire(destination)?.groupValues?.get(1)?.toLongOrNull()
            ?: throw AccessDeniedException("허용되지 않은 destination입니다")
        requireRoomAccess(principal, roomId)
    }

    private fun requirePrincipal(accessor: StompHeaderAccessor): UserPrincipal =
        (accessor.user as? UsernamePasswordAuthenticationToken)?.principal as? UserPrincipal
            ?: throw AccessDeniedException("인증되지 않은 연결입니다")

    private fun requireRoomAccess(principal: UserPrincipal, roomId: Long) {
        // 그룹 리소스의 404 은닉 원칙과 결을 맞춰, 없는 방과 권한 없는 방을 같은 메시지로 거부한다.
        val room = chatRoomMapper.findById(roomId) ?: throw AccessDeniedException("접근할 수 없는 채팅방입니다")
        val allowed = if (room.groupId != null) {
            userGroupMapper.findRole(principal.id, room.groupId) != null
        } else {
            room.userAId == principal.id || room.userBId == principal.id
        }
        if (!allowed) throw AccessDeniedException("접근할 수 없는 채팅방입니다")
    }

    companion object {
        private val TOPIC_PATTERN = Regex("""/topic/chat-rooms/(\d+)""")
        private val TYPING_PATTERN = Regex("""/app/chat-rooms/(\d+)/typing""")
        private const val USER_NOTIFICATIONS_DESTINATION = "/user/queue/notifications"
    }
}

// convertAndSendToUser의 사용자 라우팅 키는 세션 Principal.name인데, 기본
// UsernamePasswordAuthenticationToken의 name은 UserDetails.username(이메일)이다.
// 알림 발송부(NotificationBroadcaster)가 수신자 userId만 알고 있으므로 name을 userId 문자열로 맞춘다.
private class StompUserToken(private val user: UserPrincipal) :
    UsernamePasswordAuthenticationToken(user, null, user.authorities) {
    override fun getName(): String = user.id.toString()
}
