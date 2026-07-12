package kr.hhp227.groupsns_webapp.report.dto

import kr.hhp227.groupsns_webapp.report.PostReportRow
import kr.hhp227.groupsns_webapp.report.ReportStatus
import kr.hhp227.groupsns_webapp.report.UserReportRow
import java.time.OffsetDateTime
import javax.validation.constraints.Size

// 사용자/게시글 신고 접수 공용. 신고 사유는 선택 - 웹은 사유 없이도 접수할 수 있다.
data class ReportRequest(
    @field:Size(max = 500) val reason: String? = null
)

// 신고 처리(확인/기각). PENDING으로 되돌리는 것은 허용하지 않는다(서비스에서 검증).
data class ProcessReportRequest(
    val status: ReportStatus
)

data class PostReportResponse(
    val id: Long,
    val postId: Long,
    // 신고함 목록에서 게시글로 이동하지 않고도 무엇이 신고됐는지 알 수 있도록 본문 앞부분만 자른다.
    val postTextPreview: String,
    val postAuthorId: Long,
    val postAuthorName: String,
    val reporterId: Long,
    val reporterName: String,
    val reason: String?,
    val status: ReportStatus,
    val createdAt: OffsetDateTime,
    val processedAt: OffsetDateTime?
) {
    companion object {
        fun from(row: PostReportRow) = PostReportResponse(
            id = row.id,
            postId = row.postId,
            postTextPreview = row.postText.take(100),
            postAuthorId = row.postAuthorId,
            postAuthorName = row.postAuthorName,
            reporterId = row.reporterId,
            reporterName = row.reporterName,
            reason = row.reason,
            status = row.status,
            createdAt = row.createdAt,
            processedAt = row.processedAt
        )
    }
}

data class UserReportResponse(
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
    // 피신고자의 누적 신고 수(상태 무관) - 상습 피신고자 식별용.
    val reportedTotalCount: Long
) {
    companion object {
        fun from(row: UserReportRow) = UserReportResponse(
            id = row.id,
            reporterId = row.reporterId,
            reporterName = row.reporterName,
            reportedId = row.reportedId,
            reportedName = row.reportedName,
            reportedProfileImg = row.reportedProfileImg,
            reason = row.reason,
            status = row.status,
            createdAt = row.createdAt,
            processedAt = row.processedAt,
            reportedTotalCount = row.reportedTotalCount
        )
    }
}
