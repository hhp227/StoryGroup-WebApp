package kr.hhp227.groupsns_webapp.realtime

import kr.hhp227.groupsns_webapp.chat.ChatRoomMapper
import kr.hhp227.groupsns_webapp.common.exception.ChatRoomNotFoundException
import kr.hhp227.groupsns_webapp.group.UserGroupMapper
import kr.hhp227.groupsns_webapp.security.UserPrincipal
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

// 통화 로스터 스냅숏 REST(페이스톡 전환) — "통화 중 N명" 미리보기용.
// rtc 토픽 구독은 곧 통화 입장이라, 입장 없이 진행 여부만 볼 수 있는 읽기 경로가 필요하다
// (웹 사이드바 라이브 카드·채팅 페이지의 시작/참가 분기). 인메모리 트래커라 단일 인스턴스 전제(D3)를 공유한다.
@RestController
@RequestMapping("/api/rtc")
class RtcRosterController(
    private val tracker: RtcRoomTracker,
    private val chatRoomMapper: ChatRoomMapper,
    private val userGroupMapper: UserGroupMapper
) {
    @GetMapping("/chat-rooms/{chatRoomId}/roster")
    fun chatRoomRoster(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable chatRoomId: Long
    ): List<RtcPeer> {
        // 접근권은 StompAuthChannelInterceptor의 방 규칙 미러 — 없는 방과 권한 없는 방을 같은 404로 은닉
        val room = chatRoomMapper.findById(chatRoomId) ?: throw ChatRoomNotFoundException()
        val allowed = if (room.groupId != null) {
            userGroupMapper.findRole(principal.id, room.groupId) != null
        } else {
            room.userAId == principal.id || room.userBId == principal.id
        }
        if (!allowed) throw ChatRoomNotFoundException()
        return tracker.roster("chat-rooms/$chatRoomId")
    }
}
