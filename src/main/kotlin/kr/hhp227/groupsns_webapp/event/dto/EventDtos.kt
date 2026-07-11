package kr.hhp227.groupsns_webapp.event.dto

import kr.hhp227.groupsns_webapp.event.EventRow
import kr.hhp227.groupsns_webapp.event.EventRsvpRow
import kr.hhp227.groupsns_webapp.event.RsvpStatus
import java.time.OffsetDateTime
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

data class CreateEventRequest(
    @field:NotBlank @field:Size(max = 100) val title: String,
    @field:Size(max = 2000) val description: String? = null,
    @field:Size(max = 200) val location: String? = null,
    val startsAt: OffsetDateTime,
    // null: 종료 시각 없는 일정(모임 시작 시각만 공지하는 경우)
    val endsAt: OffsetDateTime? = null
)

data class UpdateEventRequest(
    @field:NotBlank @field:Size(max = 100) val title: String,
    @field:Size(max = 2000) val description: String? = null,
    @field:Size(max = 200) val location: String? = null,
    val startsAt: OffsetDateTime,
    val endsAt: OffsetDateTime? = null
)

data class RsvpRequest(
    val status: RsvpStatus
)

data class EventResponse(
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
    // 조회자 본인의 RSVP 상태. null이면 아직 응답 안 함.
    val myRsvp: RsvpStatus?
) {
    companion object {
        fun from(row: EventRow) = EventResponse(
            id = row.id,
            groupId = row.groupId,
            userId = row.userId,
            authorName = row.authorName,
            authorProfileImg = row.authorProfileImg,
            title = row.title,
            description = row.description,
            location = row.location,
            startsAt = row.startsAt,
            endsAt = row.endsAt,
            createdAt = row.createdAt,
            goingCount = row.goingCount,
            maybeCount = row.maybeCount,
            notGoingCount = row.notGoingCount,
            myRsvp = row.myRsvp?.let { RsvpStatus.valueOf(it) }
        )
    }
}

data class EventAttendeeResponse(
    val userId: Long,
    val name: String,
    val profileImg: String?,
    val status: RsvpStatus
) {
    companion object {
        fun from(row: EventRsvpRow) = EventAttendeeResponse(
            userId = row.userId,
            name = row.name,
            profileImg = row.profileImg,
            status = RsvpStatus.valueOf(row.status)
        )
    }
}

// 단건 조회 전용: 목록 응답(EventResponse)에 참석자 명단을 더한 형태.
data class EventDetailResponse(
    val event: EventResponse,
    val attendees: List<EventAttendeeResponse>
)
