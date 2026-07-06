package kr.hhp227.groupsns_webapp.realtime

import kr.hhp227.groupsns_webapp.chat.ChatRoomMapper
import kr.hhp227.groupsns_webapp.security.UserPrincipal
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Controller
import java.security.Principal

// WebRTC 시그널링 릴레이(Phase 7). Typing처럼 DB를 안 거치는 휘발성 경로 —
// 보내는 쪽의 방 접근권은 StompAuthChannelInterceptor의 SEND 인가가 이 핸들러 진입 전에 보장하고,
// 여기서는 수신자 검증과 표적 전달만 한다(D4).
@Controller
class RtcSignalController(
    private val tracker: RtcRoomTracker,
    private val broadcaster: RtcBroadcaster,
    private val chatRoomMapper: ChatRoomMapper
) {
    @MessageMapping("/rtc/meetings/{meetingId}/signal")
    fun meetingSignal(@DestinationVariable meetingId: Long, @Payload request: RtcSignalRequest, principal: Principal) =
        relay("meetings/$meetingId", request, principal)

    @MessageMapping("/rtc/chat-rooms/{chatRoomId}/signal")
    fun chatRoomSignal(@DestinationVariable chatRoomId: Long, @Payload request: RtcSignalRequest, principal: Principal) =
        relay("chat-rooms/$chatRoomId", request, principal)

    // DM 통화 벨울림(D6). 상대는 통화 화면에 없어도 헤더의 전역 알림 소켓으로 받는다.
    // DM 방이 아니면 무시 — 그룹 회의는 기존 MEETING_STARTED DB 알림이 이미 있다.
    @MessageMapping("/rtc/chat-rooms/{chatRoomId}/invite")
    fun invite(@DestinationVariable chatRoomId: Long, principal: Principal) {
        val user = principal.asUserPrincipal()
        val room = chatRoomMapper.findById(chatRoomId) ?: return
        if (room.groupId != null) return
        val otherUserId = when (user.id) {
            room.userAId -> room.userBId
            room.userBId -> room.userAId
            else -> null
        } ?: return
        broadcaster.relayInvite(otherUserId, CallInviteEvent(chatRoomId, user.id, user.name))
    }

    private fun relay(roomKey: String, request: RtcSignalRequest, principal: Principal) {
        // 수신자가 같은 통화에 없으면 조용히 버린다 — 통화 밖 사용자에게 신호 스팸 불가,
        // 그리고 상대가 방금 나간 경우의 늦은 ICE도 자연스럽게 소멸한다.
        if (!tracker.isInRoom(roomKey, request.toUserId)) return
        val user = principal.asUserPrincipal()
        broadcaster.relaySignal(
            request.toUserId,
            RtcSignalEvent(request.type, roomKey, user.id, user.name, request.payload)
        )
    }

    private fun Principal.asUserPrincipal(): UserPrincipal =
        (this as UsernamePasswordAuthenticationToken).principal as UserPrincipal
}
