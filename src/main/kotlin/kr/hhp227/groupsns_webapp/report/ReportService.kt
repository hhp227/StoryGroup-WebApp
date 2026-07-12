package kr.hhp227.groupsns_webapp.report

import kr.hhp227.groupsns_webapp.common.db.DbSessionMapper
import kr.hhp227.groupsns_webapp.common.exception.AlreadyReportedException
import kr.hhp227.groupsns_webapp.common.exception.ForbiddenException
import kr.hhp227.groupsns_webapp.common.exception.GroupNotFoundException
import kr.hhp227.groupsns_webapp.common.exception.PostNotFoundException
import kr.hhp227.groupsns_webapp.common.exception.ReportNotFoundException
import kr.hhp227.groupsns_webapp.common.exception.UserNotFoundException
import kr.hhp227.groupsns_webapp.group.GroupRole
import kr.hhp227.groupsns_webapp.group.UserGroupMapper
import kr.hhp227.groupsns_webapp.post.Post
import kr.hhp227.groupsns_webapp.post.PostMapper
import kr.hhp227.groupsns_webapp.report.dto.PostReportResponse
import kr.hhp227.groupsns_webapp.report.dto.UserReportResponse
import kr.hhp227.groupsns_webapp.user.UserMapper
import kr.hhp227.groupsns_webapp.user.UserRole
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// 신고 접수 + 신고 관리(PRD 14번). 게시글 신고는 그룹 관리자(방장/부방장)가,
// 사용자 신고는 앱 운영자(users.role = ADMIN)가 처리한다.
@Service
class ReportService(
    private val userReportMapper: UserReportMapper,
    private val postReportMapper: PostReportMapper,
    private val userMapper: UserMapper,
    private val postMapper: PostMapper,
    private val userGroupMapper: UserGroupMapper,
    private val dbSessionMapper: DbSessionMapper
) {
    @Transactional
    fun reportUser(userId: Long, targetUserId: Long, reason: String?) {
        if (userId == targetUserId) throw IllegalArgumentException("자기 자신은 신고할 수 없습니다")
        dbSessionMapper.setCurrentUserId(userId)
        userMapper.findById(targetUserId) ?: throw UserNotFoundException()
        if (userReportMapper.existsPending(userId, targetUserId)) throw AlreadyReportedException()
        userReportMapper.insert(userId, targetUserId, reason?.trim()?.ifEmpty { null })
    }

    @Transactional
    fun reportPost(userId: Long, groupId: Long, postId: Long, reason: String?) {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        val post = requirePostExists(groupId, postId)
        if (post.userId == userId) throw IllegalArgumentException("자기 게시글은 신고할 수 없습니다")
        if (postReportMapper.existsPending(userId, postId)) throw AlreadyReportedException("이미 신고한 게시글입니다")
        postReportMapper.insert(userId, postId, reason?.trim()?.ifEmpty { null })
    }

    @Transactional
    fun listGroupReports(userId: Long, groupId: Long, status: ReportStatus?, page: Int, size: Int): List<PostReportResponse> {
        dbSessionMapper.setCurrentUserId(userId)
        requireModerator(userId, groupId)
        return postReportMapper.findByGroup(groupId, status, size, page * size).map { PostReportResponse.from(it) }
    }

    // 처리는 상태 기록만 - 게시글 삭제/작성자 강퇴 같은 실제 조치는 관리자가 기존 기능으로 직접 한다.
    // 처리된 신고를 다시 처리(확인<->기각 변경)하는 것은 허용한다.
    @Transactional
    fun processGroupReport(userId: Long, groupId: Long, reportId: Long, status: ReportStatus): PostReportResponse {
        dbSessionMapper.setCurrentUserId(userId)
        requireModerator(userId, groupId)
        requireProcessable(status)
        postReportMapper.findByIdInGroup(reportId, groupId) ?: throw ReportNotFoundException()
        postReportMapper.updateStatus(reportId, status, userId)
        return PostReportResponse.from(postReportMapper.findByIdInGroup(reportId, groupId)!!)
    }

    @Transactional
    fun listUserReports(adminId: Long, status: ReportStatus?, page: Int, size: Int): List<UserReportResponse> {
        dbSessionMapper.setCurrentUserId(adminId)
        requireAdmin(adminId)
        return userReportMapper.findAll(status, size, page * size).map { UserReportResponse.from(it) }
    }

    @Transactional
    fun processUserReport(adminId: Long, reportId: Long, status: ReportStatus): UserReportResponse {
        dbSessionMapper.setCurrentUserId(adminId)
        requireAdmin(adminId)
        requireProcessable(status)
        userReportMapper.findById(reportId) ?: throw ReportNotFoundException()
        userReportMapper.updateStatus(reportId, status, adminId)
        return UserReportResponse.from(userReportMapper.findById(reportId)!!)
    }

    private fun requireMembership(userId: Long, groupId: Long): GroupRole =
        userGroupMapper.findRole(userId, groupId) ?: throw GroupNotFoundException()

    private fun requireModerator(userId: Long, groupId: Long) {
        if (!requireMembership(userId, groupId).isModerator) throw ForbiddenException()
    }

    // 운영자 API는 그룹 스코프가 아니라서 존재를 숨길 이유가 없다 - 404 관례 대신 403.
    private fun requireAdmin(userId: Long) {
        val user = userMapper.findById(userId) ?: throw UserNotFoundException()
        if (user.role != UserRole.ADMIN) throw ForbiddenException()
    }

    private fun requireProcessable(status: ReportStatus) {
        require(status != ReportStatus.PENDING) { "처리 상태는 확인(RESOLVED) 또는 기각(DISMISSED)만 가능합니다" }
    }

    private fun requirePostExists(groupId: Long, postId: Long): Post =
        postMapper.findById(postId)?.takeIf { it.groupId == groupId } ?: throw PostNotFoundException()
}
