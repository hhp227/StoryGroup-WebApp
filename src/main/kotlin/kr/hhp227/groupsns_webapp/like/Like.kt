package kr.hhp227.groupsns_webapp.like

import java.time.OffsetDateTime

class NewLikeRecord(
    val userId: Long,
    val postId: Long
)

// 좋아요를 누른 사용자 정보(name/profile_img)를 조인해서 가져오는 조회 전용 row.
data class LikeFeedRow(
    val userId: Long,
    val authorName: String,
    val authorProfileImg: String?,
    val createdAt: OffsetDateTime
)
