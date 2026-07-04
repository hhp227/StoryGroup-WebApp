package kr.hhp227.groupsns_webapp.like.dto

import kr.hhp227.groupsns_webapp.like.LikeFeedRow
import java.time.OffsetDateTime

data class LikeResponse(
    val userId: Long,
    val authorName: String,
    val authorProfileImg: String?,
    val createdAt: OffsetDateTime
) {
    companion object {
        fun from(row: LikeFeedRow) = LikeResponse(
            userId = row.userId,
            authorName = row.authorName,
            authorProfileImg = row.authorProfileImg,
            createdAt = row.createdAt
        )
    }
}
