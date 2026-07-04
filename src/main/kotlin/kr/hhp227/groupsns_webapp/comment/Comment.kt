package kr.hhp227.groupsns_webapp.comment

import java.time.OffsetDateTime

// MyBatis useGeneratedKeys는 결과를 세팅할 mutable 프로퍼티가 필요해 삽입 전용으로 분리한 홀더.
class NewReplyRecord(
    val userId: Long,
    val parentReplyId: Long?,
    val reply: String
) {
    var id: Long = 0
}

// replys와 1:1인 user_replys(레거시 (user_id, post_id, reply_id) 매핑 테이블) 삽입 전용 홀더.
class NewUserReplyRecord(
    val replyId: Long,
    val userId: Long,
    val postId: Long
)

class CommentUpdate(
    val id: Long,
    val reply: String
)

// 작성자 정보(name/profile_img)와 소속 게시글(post_id, user_replys 경유)을 조인해서 가져오는 조회 전용 row. 목록/단건 조회 공용.
data class CommentFeedRow(
    val id: Long,
    val postId: Long,
    val userId: Long,
    val authorName: String,
    val authorProfileImg: String?,
    val parentReplyId: Long?,
    val reply: String,
    val createdAt: OffsetDateTime
)
