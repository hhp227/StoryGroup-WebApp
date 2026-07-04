package kr.hhp227.groupsns_webapp.like

import kr.hhp227.groupsns_webapp.common.db.DbSessionMapper
import kr.hhp227.groupsns_webapp.common.exception.AlreadyLikedException
import kr.hhp227.groupsns_webapp.common.exception.GroupNotFoundException
import kr.hhp227.groupsns_webapp.common.exception.PostNotFoundException
import kr.hhp227.groupsns_webapp.group.UserGroupMapper
import kr.hhp227.groupsns_webapp.like.dto.LikeResponse
import kr.hhp227.groupsns_webapp.post.PostMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LikeService(
    private val likeMapper: LikeMapper,
    private val postMapper: PostMapper,
    private val userGroupMapper: UserGroupMapper,
    private val dbSessionMapper: DbSessionMapper
) {
    @Transactional
    fun likePost(userId: Long, groupId: Long, postId: Long) {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        requirePostExists(groupId, postId)
        if (likeMapper.exists(userId, postId)) throw AlreadyLikedException()
        likeMapper.insert(NewLikeRecord(userId, postId))
    }

    @Transactional
    fun unlikePost(userId: Long, groupId: Long, postId: Long) {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        requirePostExists(groupId, postId)
        likeMapper.delete(userId, postId)
    }

    @Transactional
    fun listLikes(userId: Long, groupId: Long, postId: Long, page: Int, size: Int): List<LikeResponse> {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        requirePostExists(groupId, postId)
        return likeMapper.findFeedByPost(postId, size, page * size).map { LikeResponse.from(it) }
    }

    private fun requireMembership(userId: Long, groupId: Long) {
        userGroupMapper.findRole(userId, groupId) ?: throw GroupNotFoundException()
    }

    private fun requirePostExists(groupId: Long, postId: Long) {
        postMapper.findById(postId)?.takeIf { it.groupId == groupId } ?: throw PostNotFoundException()
    }
}
