package kr.hhp227.groupsns_webapp.realtime

import kr.hhp227.groupsns_webapp.chat.dto.MessageResponse

enum class ChatSocketEventType { MESSAGE_CREATED, MESSAGE_UPDATED, MESSAGE_DELETED }

// 채팅 토픽(/topic/chat-rooms/{id})으로 나가는 이벤트 envelope.
// type으로 구분하므로 나중에 TYPING/READ 같은 이벤트를 같은 토픽에 추가할 수 있다(설계 문서 D5).
data class ChatSocketEvent(
    val type: ChatSocketEventType,
    val chatRoomId: Long,
    val message: MessageResponse? = null,
    val messageId: Long? = null
) {
    companion object {
        fun created(message: MessageResponse) =
            ChatSocketEvent(ChatSocketEventType.MESSAGE_CREATED, message.chatRoomId, message = message)

        fun updated(message: MessageResponse) =
            ChatSocketEvent(ChatSocketEventType.MESSAGE_UPDATED, message.chatRoomId, message = message)

        // 삭제는 soft delete 이후라 본문을 다시 조회할 수 없어 id만 내보낸다.
        fun deleted(chatRoomId: Long, messageId: Long) =
            ChatSocketEvent(ChatSocketEventType.MESSAGE_DELETED, chatRoomId, messageId = messageId)
    }
}
