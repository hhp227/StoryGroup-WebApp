package kr.hhp227.groupsns_webapp.event

import kr.hhp227.groupsns_webapp.common.db.DbSessionMapper
import kr.hhp227.groupsns_webapp.common.exception.EventNotFoundException
import kr.hhp227.groupsns_webapp.common.exception.ForbiddenException
import kr.hhp227.groupsns_webapp.common.exception.GroupNotFoundException
import kr.hhp227.groupsns_webapp.event.dto.CreateEventRequest
import kr.hhp227.groupsns_webapp.event.dto.EventAttendeeResponse
import kr.hhp227.groupsns_webapp.event.dto.EventDetailResponse
import kr.hhp227.groupsns_webapp.event.dto.EventResponse
import kr.hhp227.groupsns_webapp.event.dto.UpdateEventRequest
import kr.hhp227.groupsns_webapp.group.GroupRole
import kr.hhp227.groupsns_webapp.group.UserGroupMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class EventService(
    private val eventMapper: EventMapper,
    private val eventRsvpMapper: EventRsvpMapper,
    private val userGroupMapper: UserGroupMapper,
    private val dbSessionMapper: DbSessionMapper
) {
    @Transactional
    fun createEvent(userId: Long, groupId: Long, request: CreateEventRequest): EventResponse {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        requireValidRange(request.startsAt, request.endsAt)

        val record = NewEventRecord(
            groupId, userId, request.title.trim(),
            request.description?.trim()?.ifEmpty { null },
            request.location?.trim()?.ifEmpty { null },
            request.startsAt, request.endsAt
        )
        eventMapper.insert(record)
        // 작성자는 자동 참석 처리 - 만든 사람이 "참석 0명"인 일정은 어색하다.
        eventRsvpMapper.upsert(record.id, userId, RsvpStatus.GOING.name)
        return loadEvent(record.id, userId)
    }

    @Transactional
    fun listEvents(userId: Long, groupId: Long, from: OffsetDateTime, to: OffsetDateTime): List<EventResponse> {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        requireValidRange(from, to)
        return eventMapper.findByGroupBetween(groupId, from, to, userId).map { EventResponse.from(it) }
    }

    @Transactional
    fun listUpcomingEvents(userId: Long, groupId: Long, size: Int): List<EventResponse> {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        return eventMapper.findUpcomingByGroup(groupId, userId, size).map { EventResponse.from(it) }
    }

    @Transactional
    fun getEvent(userId: Long, groupId: Long, eventId: Long): EventDetailResponse {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        val row = requireEventExists(groupId, eventId, userId)
        return EventDetailResponse(
            event = EventResponse.from(row),
            attendees = eventRsvpMapper.findByEvent(eventId).map { EventAttendeeResponse.from(it) }
        )
    }

    @Transactional
    fun updateEvent(userId: Long, groupId: Long, eventId: Long, request: UpdateEventRequest): EventResponse {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        val row = requireEventExists(groupId, eventId, userId)
        if (row.userId != userId) throw ForbiddenException()
        requireValidRange(request.startsAt, request.endsAt)

        val updated = eventMapper.update(
            EventUpdate(
                eventId, request.title.trim(),
                request.description?.trim()?.ifEmpty { null },
                request.location?.trim()?.ifEmpty { null },
                request.startsAt, request.endsAt
            )
        )
        if (updated == 0) throw EventNotFoundException()
        return loadEvent(eventId, userId)
    }

    // 삭제는 작성자 본인 또는 방장/부방장(댓글과 동일한 규칙).
    @Transactional
    fun deleteEvent(userId: Long, groupId: Long, eventId: Long) {
        dbSessionMapper.setCurrentUserId(userId)
        val role = requireMembership(userId, groupId)
        val row = requireEventExists(groupId, eventId, userId)
        if (row.userId != userId && !role.isModerator) throw ForbiddenException()
        eventMapper.softDelete(eventId)
    }

    @Transactional
    fun rsvp(userId: Long, groupId: Long, eventId: Long, status: RsvpStatus): EventResponse {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        requireEventExists(groupId, eventId, userId)
        eventRsvpMapper.upsert(eventId, userId, status.name)
        return loadEvent(eventId, userId)
    }

    @Transactional
    fun cancelRsvp(userId: Long, groupId: Long, eventId: Long): EventResponse {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        requireEventExists(groupId, eventId, userId)
        eventRsvpMapper.delete(eventId, userId)
        return loadEvent(eventId, userId)
    }

    private fun loadEvent(eventId: Long, viewerId: Long): EventResponse {
        val row = eventMapper.findById(eventId, viewerId) ?: throw EventNotFoundException()
        return EventResponse.from(row)
    }

    private fun requireMembership(userId: Long, groupId: Long): GroupRole =
        userGroupMapper.findRole(userId, groupId) ?: throw GroupNotFoundException()

    private fun requireEventExists(groupId: Long, eventId: Long, viewerId: Long): EventRow =
        eventMapper.findById(eventId, viewerId)?.takeIf { it.groupId == groupId } ?: throw EventNotFoundException()

    private fun requireValidRange(startsAt: OffsetDateTime, endsAt: OffsetDateTime?) {
        require(endsAt == null || !endsAt.isBefore(startsAt)) { "종료 시각은 시작 시각보다 빠를 수 없습니다" }
    }
}
