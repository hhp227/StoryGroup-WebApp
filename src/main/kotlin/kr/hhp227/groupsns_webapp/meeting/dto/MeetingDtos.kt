package kr.hhp227.groupsns_webapp.meeting.dto

import kr.hhp227.groupsns_webapp.meeting.Meeting
import kr.hhp227.groupsns_webapp.meeting.ParticipantFeedRow
import java.time.OffsetDateTime

data class MeetingResponse(
    val id: Long,
    val groupId: Long,
    val hostId: Long,
    val startedAt: OffsetDateTime,
    val endedAt: OffsetDateTime?
) {
    companion object {
        fun from(meeting: Meeting) = MeetingResponse(
            id = meeting.id,
            groupId = meeting.groupId,
            hostId = meeting.hostId,
            startedAt = meeting.startedAt,
            endedAt = meeting.endedAt
        )
    }
}

data class ParticipantResponse(
    val userId: Long,
    val authorName: String,
    val authorProfileImg: String?,
    val joinedAt: OffsetDateTime,
    val leftAt: OffsetDateTime?
) {
    companion object {
        fun from(row: ParticipantFeedRow) = ParticipantResponse(
            userId = row.userId,
            authorName = row.authorName,
            authorProfileImg = row.authorProfileImg,
            joinedAt = row.joinedAt,
            leftAt = row.leftAt
        )
    }
}
