package kr.hhp227.groupsns_webapp.post

import kr.hhp227.groupsns_webapp.common.db.DbSessionMapper
import kr.hhp227.groupsns_webapp.common.exception.ForbiddenException
import kr.hhp227.groupsns_webapp.common.exception.GroupNotFoundException
import kr.hhp227.groupsns_webapp.common.exception.PostNotFoundException
import kr.hhp227.groupsns_webapp.group.GroupMapper
import kr.hhp227.groupsns_webapp.group.GroupRole
import kr.hhp227.groupsns_webapp.group.UserGroupMapper
import kr.hhp227.groupsns_webapp.notification.NotificationService
import kr.hhp227.groupsns_webapp.notification.NotificationTargetType
import kr.hhp227.groupsns_webapp.notification.NotificationType
import kr.hhp227.groupsns_webapp.post.dto.CreatePostRequest
import kr.hhp227.groupsns_webapp.post.dto.GroupNoticesResponse
import kr.hhp227.groupsns_webapp.post.dto.GroupPhotoResponse
import kr.hhp227.groupsns_webapp.post.dto.GroupPhotosResponse
import kr.hhp227.groupsns_webapp.post.dto.NoticeSummaryResponse
import kr.hhp227.groupsns_webapp.post.dto.PostResponse
import kr.hhp227.groupsns_webapp.post.dto.UpdatePostRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostService(
    private val postMapper: PostMapper,
    private val imageMapper: ImageMapper,
    private val userGroupMapper: UserGroupMapper,
    private val groupMapper: GroupMapper,
    private val notificationService: NotificationService,
    private val dbSessionMapper: DbSessionMapper
) {
    @Transactional
    fun createPost(userId: Long, groupId: Long, request: CreatePostRequest): PostResponse {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)

        val record = NewPostRecord(groupId, userId, request.text)
        postMapper.insert(record)
        request.images?.forEach { imageMapper.insert(NewImageRecord(record.id, userId, it)) }
        request.videos?.forEach { imageMapper.insert(NewImageRecord(record.id, userId, it, MEDIA_TYPE_VIDEO)) }

        notifyGroupMembersExcept(userId, groupId, NotificationType.NEW_POST, record.id)

        return loadPost(record.id, groupId)
    }

    // 라운지는 전 회원이 자동 가입돼 있어 이벤트마다 전체에게 알리면 스팸이 되므로 제외한다.
    private fun notifyGroupMembersExcept(actorId: Long, groupId: Long, type: NotificationType, postId: Long) {
        val group = groupMapper.findById(groupId) ?: return
        if (group.isLounge) return
        val recipientIds = userGroupMapper.findMembers(groupId).map { it.userId }.filter { it != actorId }
        notificationService.notifyAll(recipientIds, type, NotificationTargetType.POST, postId)
    }

    // 공지 지정/해제는 방장/부방장 전용(PRD 8번 "공지: 관리자만 작성"). 지정 시에만 NOTICE 알림을 보낸다.
    @Transactional
    fun setNotice(userId: Long, groupId: Long, postId: Long, notice: Boolean): PostResponse {
        dbSessionMapper.setCurrentUserId(userId)
        val role = requireMembership(userId, groupId)
        if (!role.isModerator) throw ForbiddenException("공지는 방장/부방장만 지정할 수 있습니다")
        postMapper.findById(postId)?.takeIf { it.groupId == groupId } ?: throw PostNotFoundException()

        val updated = postMapper.setNotice(postId, notice)
        if (updated == 0) throw PostNotFoundException()
        if (notice) notifyGroupMembersExcept(userId, groupId, NotificationType.NOTICE, postId)
        return loadPost(postId, groupId)
    }

    @Transactional
    fun listPosts(userId: Long, groupId: Long, page: Int, size: Int): List<PostResponse> {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)

        val rows = postMapper.findFeedByGroup(groupId, size, page * size)
        if (rows.isEmpty()) return emptyList()
        val imagesByPost = imageMapper.findByPostIds(rows.map { it.id }).groupBy { it.postId }
        return rows.map { PostResponse.from(it, imagesByPost[it.id].orEmpty()) }
    }

    // 공지 목록: 사이드바 패널(최근 N건)과 공지 전체 페이지가 공용으로 쓴다.
    // 공지는 피드에서 제외되므로(findFeedByGroup) 여기가 공지의 유일한 목록 창구.
    @Transactional
    fun listNotices(userId: Long, groupId: Long, page: Int, size: Int): GroupNoticesResponse {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)

        val totalCount = postMapper.countNotices(groupId)
        if (totalCount == 0L) return GroupNoticesResponse(0, emptyList())
        val rows = postMapper.findNotices(groupId, size, page * size)
        return GroupNoticesResponse(totalCount, rows.map { NoticeSummaryResponse.from(it) })
    }

    // 그룹 앨범(파생 뷰): 별도 앨범 엔티티 없이 게시글 첨부 이미지를 모아서 보여준다.
    @Transactional
    fun listPhotos(userId: Long, groupId: Long, page: Int, size: Int): GroupPhotosResponse {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)

        val totalCount = imageMapper.countGroupPhotos(groupId)
        if (totalCount == 0L) return GroupPhotosResponse(0, emptyList())
        val rows = imageMapper.findGroupPhotos(groupId, size, page * size)
        return GroupPhotosResponse(totalCount, rows.map { GroupPhotoResponse.from(it) })
    }

    @Transactional
    fun getPost(userId: Long, groupId: Long, postId: Long): PostResponse {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        return loadPost(postId, groupId)
    }

    @Transactional
    fun updatePost(userId: Long, groupId: Long, postId: Long, request: UpdatePostRequest): PostResponse {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        requirePostOwner(userId, groupId, postId)

        val updated = postMapper.update(PostUpdate(postId, request.text))
        if (updated == 0) throw PostNotFoundException()

        request.images?.let { urls ->
            imageMapper.deleteByPostIdAndType(postId, MEDIA_TYPE_IMAGE)
            urls.forEach { imageMapper.insert(NewImageRecord(postId, userId, it)) }
        }
        request.videos?.let { urls ->
            imageMapper.deleteByPostIdAndType(postId, MEDIA_TYPE_VIDEO)
            urls.forEach { imageMapper.insert(NewImageRecord(postId, userId, it, MEDIA_TYPE_VIDEO)) }
        }
        return loadPost(postId, groupId)
    }

    @Transactional
    fun deletePost(userId: Long, groupId: Long, postId: Long) {
        dbSessionMapper.setCurrentUserId(userId)
        val role = requireMembership(userId, groupId)
        requirePostDeletable(userId, groupId, postId, role)
        postMapper.softDelete(postId)
    }

    private fun loadPost(postId: Long, groupId: Long): PostResponse {
        val row = postMapper.findFeedRowById(postId, groupId) ?: throw PostNotFoundException()
        val attachments = imageMapper.findByPostId(postId)
        return PostResponse.from(row, attachments)
    }

    private fun requireMembership(userId: Long, groupId: Long): GroupRole =
        userGroupMapper.findRole(userId, groupId) ?: throw GroupNotFoundException()

    private fun requirePostOwner(userId: Long, groupId: Long, postId: Long) {
        val post = postMapper.findById(postId)?.takeIf { it.groupId == groupId } ?: throw PostNotFoundException()
        if (post.userId != userId) throw ForbiddenException()
    }

    // 삭제는 작성자 본인 또는 방장/부방장이 할 수 있다. 수정(requirePostOwner)은 그대로 작성자 본인만 허용.
    private fun requirePostDeletable(userId: Long, groupId: Long, postId: Long, role: GroupRole) {
        val post = postMapper.findById(postId)?.takeIf { it.groupId == groupId } ?: throw PostNotFoundException()
        if (post.userId != userId && !role.isModerator) throw ForbiddenException()
    }
}
