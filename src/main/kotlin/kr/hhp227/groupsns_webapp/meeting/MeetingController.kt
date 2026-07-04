package kr.hhp227.groupsns_webapp.meeting

import kr.hhp227.groupsns_webapp.meeting.dto.MeetingResponse
import kr.hhp227.groupsns_webapp.meeting.dto.ParticipantResponse
import kr.hhp227.groupsns_webapp.security.UserPrincipal
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/groups/{groupId}/meetings")
class MeetingController(private val meetingService: MeetingService) {

    @PostMapping
    fun createMeeting(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long
    ): ResponseEntity<MeetingResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(meetingService.createMeeting(principal.id, groupId))

    @GetMapping
    fun listMeetings(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): List<MeetingResponse> = meetingService.listMeetings(principal.id, groupId, page, size.coerceIn(1, 50))

    @GetMapping("/{meetingId}")
    fun getMeeting(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable meetingId: Long
    ): MeetingResponse = meetingService.getMeeting(principal.id, groupId, meetingId)

    @PostMapping("/{meetingId}/join")
    fun joinMeeting(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable meetingId: Long
    ): ResponseEntity<Void> {
        meetingService.joinMeeting(principal.id, groupId, meetingId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{meetingId}/leave")
    fun leaveMeeting(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable meetingId: Long
    ): ResponseEntity<Void> {
        meetingService.leaveMeeting(principal.id, groupId, meetingId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{meetingId}/end")
    fun endMeeting(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable meetingId: Long
    ): ResponseEntity<Void> {
        meetingService.endMeeting(principal.id, groupId, meetingId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{meetingId}/participants")
    fun listParticipants(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable meetingId: Long
    ): List<ParticipantResponse> = meetingService.listParticipants(principal.id, groupId, meetingId)
}
