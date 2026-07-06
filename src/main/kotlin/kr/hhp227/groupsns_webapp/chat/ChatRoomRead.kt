package kr.hhp227.groupsns_webapp.chat

import java.time.OffsetDateTime

// 방×사용자당 한 행 — 이 사용자가 이 방에서 마지막으로 읽은 메시지 위치.
data class ChatRoomRead(
    val chatRoomId: Long,
    val userId: Long,
    val lastReadMessageId: Long,
    val updatedAt: OffsetDateTime
)
