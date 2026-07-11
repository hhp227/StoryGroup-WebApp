package kr.hhp227.groupsns_webapp.chat

import kr.hhp227.groupsns_webapp.chat.dto.GroupChatRoomResponse
import kr.hhp227.groupsns_webapp.security.UserPrincipal
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

// 채팅 허브(웹 /dm)용 그룹 무관 엔드포인트. 그룹 스코프의 ChatController(/api/groups/{groupId}/chat-rooms),
// DM의 DirectMessageController(/api/dm)와 달리 "내 전체" 관점이라 별도로 둔다.
@RestController
class ChatHubController(private val chatService: ChatService) {

    @GetMapping("/api/chat-rooms")
    fun listMyGroupChatRooms(@AuthenticationPrincipal principal: UserPrincipal): List<GroupChatRoomResponse> =
        chatService.listMyGroupChatRooms(principal.id)
}
