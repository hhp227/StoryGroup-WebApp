package kr.hhp227.groupsns_webapp.meeting

import java.time.OffsetDateTime

// MyBatis useGeneratedKeys는 결과를 세팅할 mutable 프로퍼티가 필요해 삽입 전용으로 분리한 홀더.
class NewParticipantRecord(
    val meetingId: Long,
    val userId: Long
) {
    var id: Long = 0
}

// 참가자 정보(name/profile_img)를 조인해서 가져오는 조회 전용 row.
data class ParticipantFeedRow(
    val userId: Long,
    val authorName: String,
    val authorProfileImg: String?,
    val joinedAt: OffsetDateTime,
    val leftAt: OffsetDateTime?
)
