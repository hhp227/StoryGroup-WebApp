package kr.hhp227.groupsns_webapp.report

import kr.hhp227.groupsns_webapp.common.db.DbSessionMapper
import kr.hhp227.groupsns_webapp.common.exception.AlreadyReportedException
import kr.hhp227.groupsns_webapp.common.exception.UserNotFoundException
import kr.hhp227.groupsns_webapp.user.UserMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// 사용자 신고 접수 MVP — 저장만 하고, 처리(관리자 신고 관리)는 PRD 미구현 항목으로 남아 있다.
@Service
class ReportService(
    private val userReportMapper: UserReportMapper,
    private val userMapper: UserMapper,
    private val dbSessionMapper: DbSessionMapper
) {
    @Transactional
    fun reportUser(userId: Long, targetUserId: Long, reason: String?) {
        if (userId == targetUserId) throw IllegalArgumentException("자기 자신은 신고할 수 없습니다")
        dbSessionMapper.setCurrentUserId(userId)
        userMapper.findById(targetUserId) ?: throw UserNotFoundException()
        if (userReportMapper.exists(userId, targetUserId)) throw AlreadyReportedException()
        userReportMapper.insert(userId, targetUserId, reason?.trim()?.ifEmpty { null })
    }
}
