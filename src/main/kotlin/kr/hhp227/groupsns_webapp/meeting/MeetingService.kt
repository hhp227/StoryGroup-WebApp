package kr.hhp227.groupsns_webapp.meeting

import kr.hhp227.groupsns_webapp.common.db.DbSessionMapper
import kr.hhp227.groupsns_webapp.common.exception.ForbiddenException
import kr.hhp227.groupsns_webapp.common.exception.GroupNotFoundException
import kr.hhp227.groupsns_webapp.common.exception.MeetingNotFoundException
import kr.hhp227.groupsns_webapp.group.UserGroupMapper
import kr.hhp227.groupsns_webapp.meeting.dto.MeetingResponse
import kr.hhp227.groupsns_webapp.meeting.dto.ParticipantResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MeetingService(
    private val meetingMapper: MeetingMapper,
    private val participantMapper: MeetingParticipantMapper,
    private val userGroupMapper: UserGroupMapper,
    private val dbSessionMapper: DbSessionMapper
) {
    @Transactional
    fun createMeeting(userId: Long, groupId: Long): MeetingResponse {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)

        val record = NewMeetingRecord(groupId, userId)
        meetingMapper.insert(record)
        participantMapper.insert(NewParticipantRecord(record.id, userId))

        val meeting = meetingMapper.findById(record.id) ?: throw MeetingNotFoundException()
        return MeetingResponse.from(meeting)
    }

    @Transactional
    fun listMeetings(userId: Long, groupId: Long, page: Int, size: Int): List<MeetingResponse> {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        return meetingMapper.findByGroup(groupId, size, page * size).map { MeetingResponse.from(it) }
    }

    @Transactional
    fun getMeeting(userId: Long, groupId: Long, meetingId: Long): MeetingResponse {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        return MeetingResponse.from(requireMeetingExists(groupId, meetingId))
    }

    @Transactional
    fun joinMeeting(userId: Long, groupId: Long, meetingId: Long) {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        val meeting = requireMeetingExists(groupId, meetingId)
        if (meeting.endedAt != null) throw IllegalArgumentException("이미 종료된 회의입니다")

        if (!participantMapper.hasActiveParticipant(meetingId, userId)) {
            participantMapper.insert(NewParticipantRecord(meetingId, userId))
        }
    }

    @Transactional
    fun leaveMeeting(userId: Long, groupId: Long, meetingId: Long) {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        requireMeetingExists(groupId, meetingId)
        participantMapper.leave(meetingId, userId)
    }

    @Transactional
    fun endMeeting(userId: Long, groupId: Long, meetingId: Long) {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        val meeting = requireMeetingExists(groupId, meetingId)
        if (meeting.hostId != userId) throw ForbiddenException()

        if (meetingMapper.end(meetingId) > 0) {
            participantMapper.leaveAllActive(meetingId)
        }
    }

    @Transactional
    fun listParticipants(userId: Long, groupId: Long, meetingId: Long): List<ParticipantResponse> {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        requireMeetingExists(groupId, meetingId)
        return participantMapper.findFeedByMeeting(meetingId).map { ParticipantResponse.from(it) }
    }

    private fun requireMembership(userId: Long, groupId: Long) {
        userGroupMapper.findRole(userId, groupId) ?: throw GroupNotFoundException()
    }

    private fun requireMeetingExists(groupId: Long, meetingId: Long): Meeting =
        meetingMapper.findById(meetingId)?.takeIf { it.groupId == groupId } ?: throw MeetingNotFoundException()
}
