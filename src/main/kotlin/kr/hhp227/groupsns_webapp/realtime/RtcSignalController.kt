package kr.hhp227.groupsns_webapp.realtime

import kr.hhp227.groupsns_webapp.chat.ChatRoomMapper
import kr.hhp227.groupsns_webapp.group.GroupMapper
import kr.hhp227.groupsns_webapp.group.UserGroupMapper
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
    private val chatRoomMapper: ChatRoomMapper,
    private val groupMapper: GroupMapper,
    private val userGroupMapper: UserGroupMapper
) {
    @MessageMapping("/rtc/meetings/{meetingId}/signal")
    fun meetingSignal(@DestinationVariable meetingId: Long, @Payload request: RtcSignalRequest, principal: Principal) =
        relay("meetings/$meetingId", request, principal)

    @MessageMapping("/rtc/chat-rooms/{chatRoomId}/signal")
    fun chatRoomSignal(@DestinationVariable chatRoomId: Long, @Payload request: RtcSignalRequest, principal: Principal) =
        relay("chat-rooms/$chatRoomId", request, principal)

    // 통화 벨울림(D6, 페이스톡 전환으로 그룹 방 포함). 수신자는 통화 화면에 없어도
    // 헤더/셸의 전역 알림 소켓으로 받는다. DM=상대 1명, 그룹 방=방 멤버(그룹원) 전원 팬아웃.
    // body는 선택(CallInviteRequest) — 빈 바디는 페이스톡(video=true)으로 간주한다(웹·구버전 앱 하위호환).
    @MessageMapping("/rtc/chat-rooms/{chatRoomId}/invite")
    fun invite(
        @DestinationVariable chatRoomId: Long,
        @Payload(required = false) request: CallInviteRequest?,
        principal: Principal
    ) {
        val user = principal.asUserPrincipal()
        val room = chatRoomMapper.findById(chatRoomId) ?: return
        // 이미 진행 중인 통화에 합류하는 경우엔 다시 울리지 않는다 — 발신자 외 인원이 로스터에 있으면 합류다.
        // (탭 순서상 발신자 본인은 이 시점에 이미 로스터에 있을 수 있어 본인은 세지 않는다)
        if (tracker.roster("chat-rooms/$chatRoomId").any { it.userId != user.id }) return
        val video = request?.video ?: true

        if (room.groupId == null) {
            val otherUserId = when (user.id) {
                room.userAId -> room.userBId
                room.userBId -> room.userAId
                else -> null
            } ?: return
            broadcaster.relayInvite(otherUserId, CallInviteEvent(chatRoomId, user.id, user.name, video = video))
        } else {
            val group = groupMapper.findById(room.groupId) ?: return
            // 라운지는 전 사용자가 자동 멤버 — 벨울림 팬아웃 대상이 아니다(통화 자체는 막지 않는다)
            if (group.isLounge) return
            val event = CallInviteEvent(chatRoomId, user.id, user.name, room.groupId, group.name, video)
            userGroupMapper.findMembers(room.groupId)
                .filter { it.userId != user.id }
                .forEach { broadcaster.relayInvite(it.userId, event) }
        }
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
