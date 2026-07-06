package kr.hhp227.groupsns_webapp.chat.dto

import kr.hhp227.groupsns_webapp.chat.ChatRoom
import kr.hhp227.groupsns_webapp.chat.ChatRoomRead
import kr.hhp227.groupsns_webapp.chat.DirectRoomRow
import kr.hhp227.groupsns_webapp.chat.MessageFeedRow
import java.time.OffsetDateTime
import javax.validation.Valid
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

// 첨부 파일 메타데이터 — url은 업로드 API(/api/images, /api/files)가 돌려준 공개 URL 문자열
// (게시글 images/프로필 profileImg와 같은 "URL 문자열" 계약).
data class MessageAttachmentPayload(
    @field:NotBlank @field:Size(max = 2048) val url: String,
    @field:Size(max = 255) val name: String? = null,
    @field:Size(max = 100) val contentType: String? = null,
    @field:Positive val size: Long? = null
)

// 첨부만 있는 메시지는 text 생략 가능 — 둘 다 비면 서비스에서 400(ChatService.buildNewMessage).
data class CreateMessageRequest(
    val text: String? = null,
    @field:Valid val attachment: MessageAttachmentPayload? = null
)

data class UpdateMessageRequest(
    @field:NotBlank val text: String
)

data class MessageAttachmentResponse(
    val url: String,
    val name: String?,
    val contentType: String?,
    val size: Long?
)

data class MessageResponse(
    val id: Long,
    val chatRoomId: Long,
    val userId: Long,
    val authorName: String,
    val authorProfileImg: String?,
    val text: String,
    val attachment: MessageAttachmentResponse?,
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
            attachment = row.attachmentUrl?.let {
                MessageAttachmentResponse(it, row.attachmentName, row.attachmentType, row.attachmentSize)
            },
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
