package kr.hhp227.groupsns_webapp.meeting

import java.time.OffsetDateTime

data class Meeting(
    val id: Long,
    val groupId: Long,
    val hostId: Long,
    val startedAt: OffsetDateTime,
    val endedAt: OffsetDateTime?
)

// MyBatis useGeneratedKeys는 결과를 세팅할 mutable 프로퍼티가 필요해 Meeting(불변)과 분리한 삽입 전용 홀더.
class NewMeetingRecord(
    val groupId: Long,
    val hostId: Long
) {
    var id: Long = 0
}
