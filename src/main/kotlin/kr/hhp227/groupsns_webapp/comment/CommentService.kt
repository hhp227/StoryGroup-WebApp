package kr.hhp227.groupsns_webapp.comment

import kr.hhp227.groupsns_webapp.comment.dto.CommentResponse
import kr.hhp227.groupsns_webapp.comment.dto.CreateCommentRequest
import kr.hhp227.groupsns_webapp.comment.dto.UpdateCommentRequest
import kr.hhp227.groupsns_webapp.common.db.DbSessionMapper
import kr.hhp227.groupsns_webapp.common.exception.CommentNotFoundException
import kr.hhp227.groupsns_webapp.common.exception.ForbiddenException
import kr.hhp227.groupsns_webapp.common.exception.GroupNotFoundException
import kr.hhp227.groupsns_webapp.common.exception.PostNotFoundException
import kr.hhp227.groupsns_webapp.group.GroupRole
import kr.hhp227.groupsns_webapp.group.UserGroupMapper
import kr.hhp227.groupsns_webapp.notification.NotificationService
import kr.hhp227.groupsns_webapp.notification.NotificationTargetType
import kr.hhp227.groupsns_webapp.notification.NotificationType
import kr.hhp227.groupsns_webapp.post.PostMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CommentService(
    private val commentMapper: CommentMapper,
    private val userReplyMapper: UserReplyMapper,
    private val postMapper: PostMapper,
    private val userGroupMapper: UserGroupMapper,
    private val notificationService: NotificationService,
    private val dbSessionMapper: DbSessionMapper
) {
    @Transactional
    fun createComment(userId: Long, groupId: Long, postId: Long, request: CreateCommentRequest): CommentResponse {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        val post = requirePostExists(groupId, postId)
        val parent = request.parentReplyId?.let { parentId ->
            commentMapper.findFeedRowById(parentId, postId) ?: throw CommentNotFoundException()
        }

        val record = NewReplyRecord(userId, request.parentReplyId, request.text)
        commentMapper.insert(record)
        userReplyMapper.insert(NewUserReplyRecord(record.id, userId, postId))

        // 원글 작성자 + (대댓글이면) 부모 댓글 작성자에게 알림. 본인 글/댓글에는 알리지 않고, 중복 수신도 막는다.
        setOfNotNull(post.userId, parent?.userId)
            .filter { it != userId }
            .forEach { notificationService.notify(it, NotificationType.COMMENT, NotificationTargetType.REPLY, record.id) }

        return loadComment(record.id, postId)
    }

    @Transactional
    fun listComments(userId: Long, groupId: Long, postId: Long, page: Int, size: Int): List<CommentResponse> {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        requirePostExists(groupId, postId)
        return commentMapper.findFeedByPost(postId, size, page * size).map { CommentResponse.from(it) }
    }

    @Transactional
    fun getComment(userId: Long, groupId: Long, postId: Long, commentId: Long): CommentResponse {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        requirePostExists(groupId, postId)
        return loadComment(commentId, postId)
    }

    @Transactional
    fun updateComment(userId: Long, groupId: Long, postId: Long, commentId: Long, request: UpdateCommentRequest): CommentResponse {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        requirePostExists(groupId, postId)
        requireCommentOwner(userId, postId, commentId)

        val updated = commentMapper.update(CommentUpdate(commentId, request.text))
        if (updated == 0) throw CommentNotFoundException()
        return loadComment(commentId, postId)
    }

    @Transactional
    fun deleteComment(userId: Long, groupId: Long, postId: Long, commentId: Long) {
        dbSessionMapper.setCurrentUserId(userId)
        val role = requireMembership(userId, groupId)
        requirePostExists(groupId, postId)
        requireCommentDeletable(userId, postId, commentId, role)
        commentMapper.softDelete(commentId)
    }

    private fun loadComment(commentId: Long, postId: Long): CommentResponse {
        val row = commentMapper.findFeedRowById(commentId, postId) ?: throw CommentNotFoundException()
        return CommentResponse.from(row)
    }

    private fun requireMembership(userId: Long, groupId: Long): GroupRole =
        userGroupMapper.findRole(userId, groupId) ?: throw GroupNotFoundException()

    private fun requirePostExists(groupId: Long, postId: Long) =
        postMapper.findById(postId)?.takeIf { it.groupId == groupId } ?: throw PostNotFoundException()

    private fun requireCommentOwner(userId: Long, postId: Long, commentId: Long) {
        val row = commentMapper.findFeedRowById(commentId, postId) ?: throw CommentNotFoundException()
        if (row.userId != userId) throw ForbiddenException()
    }

    // 삭제는 작성자 본인 또는 그룹 방장(OWNER)이 할 수 있다. 수정(requireCommentOwner)은 그대로 작성자 본인만 허용.
    private fun requireCommentDeletable(userId: Long, postId: Long, commentId: Long, role: GroupRole) {
        val row = commentMapper.findFeedRowById(commentId, postId) ?: throw CommentNotFoundException()
        if (row.userId != userId && role != GroupRole.OWNER) throw ForbiddenException()
    }
}
