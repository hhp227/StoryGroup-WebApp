package kr.hhp227.groupsns_webapp.report

import java.time.OffsetDateTime

// 사용자/게시글 신고 공통 처리 상태. 상태는 처리 기록일 뿐이고
// 실제 조치(게시글 삭제/차단 등)는 관리자가 기존 기능으로 직접 한다.
enum class ReportStatus {
    PENDING, RESOLVED, DISMISSED
}

// 그룹 신고함 한 행 - 신고자와 신고된 게시글 요약을 함께 조회한다.
data class PostReportRow(
    val id: Long,
    val postId: Long,
    val postText: String,
    val postAuthorId: Long,
    val postAuthorName: String,
    val reporterId: Long,
    val reporterName: String,
    val reason: String?,
    val status: ReportStatus,
    val createdAt: OffsetDateTime,
    val processedAt: OffsetDateTime?
)

// 운영자 사용자 신고 목록 한 행 - 신고자/피신고자 프로필과 피신고자 누적 신고 수를 함께 조회한다.
data class UserReportRow(
    val id: Long,
    val reporterId: Long,
    val reporterName: String,
    val reportedId: Long,
    val reportedName: String,
    val reportedProfileImg: String?,
    val reason: String?,
    val status: ReportStatus,
    val createdAt: OffsetDateTime,
    val processedAt: OffsetDateTime?,
    val reportedTotalCount: Long
)
