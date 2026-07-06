package kr.hhp227.groupsns_webapp.chat

import java.time.OffsetDateTime

data class Message(
    val id: Long,
    val chatRoomId: Long,
    val userId: Long,
    val message: String,
    val createdAt: OffsetDateTime,
    val deletedAt: OffsetDateTime?
)

// MyBatis useGeneratedKeys는 결과를 세팅할 mutable 프로퍼티가 필요해 Message(불변)와 분리한 삽입 전용 홀더.
// 첨부(V15)는 메시지당 1개 — 파일 실체는 Supabase Storage, 여기엔 URL+메타데이터만.
class NewMessageRecord(
    val chatRoomId: Long,
    val userId: Long,
    val message: String,
    val attachmentUrl: String? = null,
    val attachmentName: String? = null,
    val attachmentType: String? = null,
    val attachmentSize: Long? = null
) {
    var id: Long = 0
}

class MessageUpdate(
    val id: Long,
    val message: String
)

// 작성자 정보(name/profile_img)를 조인해서 가져오는 조회 전용 row. 목록/단건 조회 공용.
data class MessageFeedRow(
    val id: Long,
    val chatRoomId: Long,
    val userId: Long,
    val authorName: String,
    val authorProfileImg: String?,
    val message: String,
    val attachmentUrl: String?,
    val attachmentName: String?,
    val attachmentType: String?,
    val attachmentSize: Long?,
    val createdAt: OffsetDateTime
)
