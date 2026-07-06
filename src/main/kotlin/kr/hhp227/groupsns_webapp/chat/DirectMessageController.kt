package kr.hhp227.groupsns_webapp.chat

import kr.hhp227.groupsns_webapp.chat.dto.ChatRoomResponse
import kr.hhp227.groupsns_webapp.chat.dto.CreateMessageRequest
import kr.hhp227.groupsns_webapp.chat.dto.DirectRoomResponse
import kr.hhp227.groupsns_webapp.chat.dto.MarkReadRequest
import kr.hhp227.groupsns_webapp.chat.dto.MessageResponse
import kr.hhp227.groupsns_webapp.chat.dto.ReadPositionResponse
import kr.hhp227.groupsns_webapp.security.UserPrincipal
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

// 그룹에 속하지 않는 1:1 개인 채팅(DM). 그룹 채팅방은 ChatController(/api/groups/{groupId}/chat-rooms)를 그대로 씀.
@RestController
@RequestMapping("/api/dm")
class DirectMessageController(private val chatService: ChatService) {

    @GetMapping
    fun listRooms(@AuthenticationPrincipal principal: UserPrincipal): List<DirectRoomResponse> =
        chatService.listDirectRooms(principal.id)

    @PostMapping("/{otherUserId}")
    fun getOrCreateRoom(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable otherUserId: Long
    ): ChatRoomResponse = chatService.getOrCreateDirectRoom(principal.id, otherUserId)

    @GetMapping("/{chatRoomId}/messages")
    fun listMessages(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable chatRoomId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): List<MessageResponse> = chatService.listDirectMessages(principal.id, chatRoomId, page, size.coerceIn(1, 50))

    @PostMapping("/{chatRoomId}/messages")
    fun sendMessage(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable chatRoomId: Long,
        @Valid @RequestBody request: CreateMessageRequest
    ): ResponseEntity<MessageResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(chatService.sendDirectMessage(principal.id, chatRoomId, request))

    @DeleteMapping("/{chatRoomId}/messages/{messageId}")
    fun deleteMessage(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable chatRoomId: Long,
        @PathVariable messageId: Long
    ): ResponseEntity<Void> {
        chatService.deleteDirectMessage(principal.id, chatRoomId, messageId)
        return ResponseEntity.noContent().build()
    }

    // 읽음 위치 갱신은 멱등 업서트라 PUT.
    @PutMapping("/{chatRoomId}/read")
    fun markRead(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable chatRoomId: Long,
        @Valid @RequestBody request: MarkReadRequest
    ): ResponseEntity<Void> {
        chatService.markDirectRead(principal.id, chatRoomId, request.lastReadMessageId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{chatRoomId}/reads")
    fun listReads(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable chatRoomId: Long
    ): List<ReadPositionResponse> = chatService.listDirectReads(principal.id, chatRoomId)
}
