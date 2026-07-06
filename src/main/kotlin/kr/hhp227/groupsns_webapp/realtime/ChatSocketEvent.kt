package kr.hhp227.groupsns_webapp.realtime

import kr.hhp227.groupsns_webapp.chat.dto.MessageResponse

enum class ChatSocketEventType { MESSAGE_CREATED, MESSAGE_UPDATED, MESSAGE_DELETED, TYPING, PRESENCE, READ }

// PRESENCE 이벤트에 실리는 "지금 이 방을 보고 있는 사람" 한 명.
data class PresenceUser(val userId: Long, val userName: String)

// 채팅 토픽(/topic/chat-rooms/{id})으로 나가는 이벤트 envelope.
// type으로 구분하므로 나중에 READ 같은 이벤트를 같은 토픽에 추가할 수 있다(설계 문서 D5).
data class ChatSocketEvent(
    val type: ChatSocketEventType,
    val chatRoomId: Long,
    val message: MessageResponse? = null,
    val messageId: Long? = null,
    // TYPING 전용 — 누가 입력 중인지 표시할 최소 정보만 싣는다.
    val userId: Long? = null,
    val userName: String? = null,
    // PRESENCE 전용 — 증분이 아니라 항상 전체 목록을 실어 수신 측 상태가 자가 복구되게 한다.
    val users: List<PresenceUser>? = null
) {
    companion object {
        fun created(message: MessageResponse) =
            ChatSocketEvent(ChatSocketEventType.MESSAGE_CREATED, message.chatRoomId, message = message)

        fun updated(message: MessageResponse) =
            ChatSocketEvent(ChatSocketEventType.MESSAGE_UPDATED, message.chatRoomId, message = message)

        // 삭제는 soft delete 이후라 본문을 다시 조회할 수 없어 id만 내보낸다.
        fun deleted(chatRoomId: Long, messageId: Long) =
            ChatSocketEvent(ChatSocketEventType.MESSAGE_DELETED, chatRoomId, messageId = messageId)

        fun typing(chatRoomId: Long, userId: Long, userName: String) =
            ChatSocketEvent(ChatSocketEventType.TYPING, chatRoomId, userId = userId, userName = userName)

        fun presence(chatRoomId: Long, users: List<PresenceUser>) =
            ChatSocketEvent(ChatSocketEventType.PRESENCE, chatRoomId, users = users)

        // messageId = 이 사용자가 마지막으로 읽은 메시지(그 이하 전부 읽음).
        fun read(chatRoomId: Long, userId: Long, lastReadMessageId: Long) =
            ChatSocketEvent(ChatSocketEventType.READ, chatRoomId, messageId = lastReadMessageId, userId = userId)
    }
}
