package kr.hhp227.groupsns_webapp.chat

import java.time.OffsetDateTime

// groupId가 null이면 1:1 DM방(userAId/userBId로 참가자 표시), 아니면 그룹 채팅방.
data class ChatRoom(
    val id: Long,
    val groupId: Long?,
    val name: String,
    val createdAt: OffsetDateTime,
    val userAId: Long?,
    val userBId: Long?
)

// MyBatis useGeneratedKeys는 결과를 세팅할 mutable 프로퍼티가 필요해 ChatRoom(불변)과 분리한 삽입 전용 홀더.
class NewChatRoomRecord(
    val groupId: Long,
    val name: String
) {
    var id: Long = 0
}

class NewDirectRoomRecord(
    val userAId: Long,
    val userBId: Long,
    val name: String
) {
    var id: Long = 0
}

// 채팅 허브(웹 /dm)용: 내가 속한 그룹(라운지 제외)의 채팅방을 그룹 정보와 함께 가져오는 조회 전용 row.
// lastMessage*는 마지막 가시 메시지(삭제·내가 차단한 사용자 제외) — 방에 메시지가 없으면 셋 다 null,
// 첨부 전용 메시지면 text가 빈 문자열이고 type(attachment_type)으로 종류를 표시한다.
data class GroupChatRoomRow(
    val id: Long,
    val groupId: Long,
    val groupName: String,
    val name: String,
    val createdAt: OffsetDateTime,
    val unreadCount: Long,
    val lastMessageText: String?,
    val lastMessageType: String?,
    val lastMessageAt: OffsetDateTime?
)

// DM 목록에서 상대방 정보(이름/프로필)를 조인해서 가져오는 조회 전용 row.
data class DirectRoomRow(
    val id: Long,
    val otherUserId: Long,
    val otherUserName: String,
    val otherUserProfileImg: String?,
    val createdAt: OffsetDateTime,
    val unreadCount: Long,
    val lastMessageText: String?,
    val lastMessageType: String?,
    val lastMessageAt: OffsetDateTime?
)
