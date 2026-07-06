package kr.hhp227.groupsns_webapp.chat.dto

import kr.hhp227.groupsns_webapp.chat.ChatRoom
import kr.hhp227.groupsns_webapp.chat.ChatRoomRead
import kr.hhp227.groupsns_webapp.chat.DirectRoomRow
import kr.hhp227.groupsns_webapp.chat.MessageFeedRow
import java.time.OffsetDateTime
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Positive
import javax.validation.constraints.Size

data class CreateChatRoomRequest(
    @field:NotBlank @field:Size(max = 100) val name: String
)

data class ChatRoomResponse(
    val id: Long,
    val groupId: Long?,
    val name: String,
    val createdAt: OffsetDateTime
) {
    companion object {
        fun from(room: ChatRoom) = ChatRoomResponse(room.id, room.groupId, room.name, room.createdAt)
    }
}

data class DirectRoomResponse(
    val id: Long,
    val otherUserId: Long,
    val otherUserName: String,
    val otherUserProfileImg: String?,
    val createdAt: OffsetDateTime
) {
    companion object {
        fun from(row: DirectRoomRow) = DirectRoomResponse(
            id = row.id,
            otherUserId = row.otherUserId,
            otherUserName = row.otherUserName,
            otherUserProfileImg = row.otherUserProfileImg,
            createdAt = row.createdAt
        )
    }
}

data class CreateMessageRequest(
    @field:NotBlank val text: String
)

data class UpdateMessageRequest(
    @field:NotBlank val text: String
)

data class MessageResponse(
    val id: Long,
    val chatRoomId: Long,
    val userId: Long,
    val authorName: String,
    val authorProfileImg: String?,
    val text: String,
    val createdAt: OffsetDateTime
) {
    companion object {
        fun from(row: MessageFeedRow) = MessageResponse(
            id = row.id,
            chatRoomId = row.chatRoomId,
            userId = row.userId,
            authorName = row.authorName,
            authorProfileImg = row.authorProfileImg,
            text = row.message,
            createdAt = row.createdAt
        )
    }
}

data class MarkReadRequest(
    @field:Positive val lastReadMessageId: Long
)

data class ReadPositionResponse(
    val userId: Long,
    val lastReadMessageId: Long
) {
    companion object {
        fun from(read: ChatRoomRead) = ReadPositionResponse(read.userId, read.lastReadMessageId)
    }
}
