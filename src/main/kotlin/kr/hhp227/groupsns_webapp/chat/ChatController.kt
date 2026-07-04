package kr.hhp227.groupsns_webapp.chat

import kr.hhp227.groupsns_webapp.chat.dto.ChatRoomResponse
import kr.hhp227.groupsns_webapp.chat.dto.CreateChatRoomRequest
import kr.hhp227.groupsns_webapp.chat.dto.CreateMessageRequest
import kr.hhp227.groupsns_webapp.chat.dto.MessageResponse
import kr.hhp227.groupsns_webapp.chat.dto.UpdateMessageRequest
import kr.hhp227.groupsns_webapp.security.UserPrincipal
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@RequestMapping("/api/groups/{groupId}/chat-rooms")
class ChatController(private val chatService: ChatService) {

    @PostMapping
    fun createChatRoom(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @Valid @RequestBody request: CreateChatRoomRequest
    ): ResponseEntity<ChatRoomResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(chatService.createChatRoom(principal.id, groupId, request))

    @GetMapping
    fun listChatRooms(@AuthenticationPrincipal principal: UserPrincipal, @PathVariable groupId: Long): List<ChatRoomResponse> =
        chatService.listChatRooms(principal.id, groupId)

    @PostMapping("/{chatRoomId}/messages")
    fun sendMessage(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable chatRoomId: Long,
        @Valid @RequestBody request: CreateMessageRequest
    ): ResponseEntity<MessageResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(chatService.sendMessage(principal.id, groupId, chatRoomId, request))

    @GetMapping("/{chatRoomId}/messages")
    fun listMessages(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable chatRoomId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): List<MessageResponse> = chatService.listMessages(principal.id, groupId, chatRoomId, page, size.coerceIn(1, 50))

    @PatchMapping("/{chatRoomId}/messages/{messageId}")
    fun updateMessage(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable chatRoomId: Long,
        @PathVariable messageId: Long,
        @Valid @RequestBody request: UpdateMessageRequest
    ): MessageResponse = chatService.updateMessage(principal.id, groupId, chatRoomId, messageId, request)

    @DeleteMapping("/{chatRoomId}/messages/{messageId}")
    fun deleteMessage(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable chatRoomId: Long,
        @PathVariable messageId: Long
    ): ResponseEntity<Void> {
        chatService.deleteMessage(principal.id, groupId, chatRoomId, messageId)
        return ResponseEntity.noContent().build()
    }
}
