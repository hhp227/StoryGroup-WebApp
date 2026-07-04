package kr.hhp227.groupsns_webapp.comment.dto

import kr.hhp227.groupsns_webapp.comment.CommentFeedRow
import java.time.OffsetDateTime
import javax.validation.constraints.NotBlank

data class CreateCommentRequest(
    @field:NotBlank val text: String,
    // null: 최상위 댓글, 값 있음: 해당 댓글에 대한 대댓글(같은 게시글 소속이어야 함)
    val parentReplyId: Long? = null
)

data class UpdateCommentRequest(
    @field:NotBlank val text: String
)

data class CommentResponse(
    val id: Long,
    val postId: Long,
    val userId: Long,
    val authorName: String,
    val authorProfileImg: String?,
    val parentReplyId: Long?,
    val text: String,
    val createdAt: OffsetDateTime
) {
    companion object {
        fun from(row: CommentFeedRow) = CommentResponse(
            id = row.id,
            postId = row.postId,
            userId = row.userId,
            authorName = row.authorName,
            authorProfileImg = row.authorProfileImg,
            parentReplyId = row.parentReplyId,
            text = row.reply,
            createdAt = row.createdAt
        )
    }
}
