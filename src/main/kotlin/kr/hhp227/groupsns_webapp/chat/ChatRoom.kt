package kr.hhp227.groupsns_webapp.chat

import java.time.OffsetDateTime

data class ChatRoom(
    val id: Long,
    val groupId: Long,
    val name: String,
    val createdAt: OffsetDateTime
)

// MyBatis useGeneratedKeys는 결과를 세팅할 mutable 프로퍼티가 필요해 ChatRoom(불변)과 분리한 삽입 전용 홀더.
class NewChatRoomRecord(
    val groupId: Long,
    val name: String
) {
    var id: Long = 0
}
