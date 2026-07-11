package kr.hhp227.groupsns_webapp.event

import java.time.OffsetDateTime

enum class RsvpStatus { GOING, MAYBE, NOT_GOING }

// MyBatis useGeneratedKeys는 결과를 세팅할 mutable 프로퍼티가 필요해 삽입 전용으로 분리한 홀더.
class NewEventRecord(
    val groupId: Long,
    val userId: Long,
    val title: String,
    val description: String?,
    val location: String?,
    val startsAt: OffsetDateTime,
    val endsAt: OffsetDateTime?
) {
    var id: Long = 0
}

class EventUpdate(
    val id: Long,
    val title: String,
    val description: String?,
    val location: String?,
    val startsAt: OffsetDateTime,
    val endsAt: OffsetDateTime?
)

// 작성자 정보 + RSVP 집계 + 조회자 본인의 RSVP 상태까지 조인한 조회 전용 row. 목록/단건 공용.
data class EventRow(
    val id: Long,
    val groupId: Long,
    val userId: Long,
    val authorName: String,
    val authorProfileImg: String?,
    val title: String,
    val description: String?,
    val location: String?,
    val startsAt: OffsetDateTime,
    val endsAt: OffsetDateTime?,
    val createdAt: OffsetDateTime,
    val goingCount: Long,
    val maybeCount: Long,
    val notGoingCount: Long,
    val myRsvp: String?
)

// 일정 상세의 참석자 한 줄. 멤버 목록과 같은 성격이라 차단 여부와 무관하게 표시한다.
data class EventRsvpRow(
    val userId: Long,
    val name: String,
    val profileImg: String?,
    val status: String
)
