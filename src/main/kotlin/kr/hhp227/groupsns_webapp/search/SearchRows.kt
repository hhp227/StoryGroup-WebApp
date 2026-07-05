package kr.hhp227.groupsns_webapp.search

import java.time.OffsetDateTime

// 검색 결과 조회 전용 row들. 목록 화면과 달리 "어느 그룹에서 나온 결과인지"가 필요해
// group_name을 함께 조인해 온다.
data class GroupSearchRow(
    val id: Long,
    val name: String,
    val image: String?,
    val description: String?
)

data class PostSearchRow(
    val id: Long,
    val groupId: Long,
    val groupName: String,
    val authorName: String,
    val text: String,
    val createdAt: OffsetDateTime
)

data class FileSearchRow(
    val id: Long,
    val groupId: Long,
    val groupName: String,
    val name: String,
    val url: String,
    val createdAt: OffsetDateTime
)

// DM 메시지는 groupId/groupName이 NULL(그룹 소속이 아님).
data class MessageSearchRow(
    val id: Long,
    val chatRoomId: Long,
    val groupId: Long?,
    val groupName: String?,
    val authorName: String,
    val message: String,
    val createdAt: OffsetDateTime
)

data class UserSearchRow(
    val id: Long,
    val name: String,
    val profileImg: String?,
    val statusMessage: String?
)
