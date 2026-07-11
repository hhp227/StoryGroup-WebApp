package kr.hhp227.groupsns_webapp.event

import kr.hhp227.groupsns_webapp.event.dto.CreateEventRequest
import kr.hhp227.groupsns_webapp.event.dto.EventDetailResponse
import kr.hhp227.groupsns_webapp.event.dto.EventResponse
import kr.hhp227.groupsns_webapp.event.dto.RsvpRequest
import kr.hhp227.groupsns_webapp.event.dto.UpdateEventRequest
import kr.hhp227.groupsns_webapp.security.UserPrincipal
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime
import javax.validation.Valid

@RestController
@RequestMapping("/api/groups/{groupId}/events")
class EventController(private val eventService: EventService) {

    @PostMapping
    fun createEvent(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @Valid @RequestBody request: CreateEventRequest
    ): ResponseEntity<EventResponse> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(eventService.createEvent(principal.id, groupId, request))

    // 캘린더 범위 조회(월 뷰가 from=월초, to=다음달 초를 넘긴다). starts_at 기준 [from, to).
    @GetMapping
    fun listEvents(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: OffsetDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: OffsetDateTime
    ): List<EventResponse> = eventService.listEvents(principal.id, groupId, from, to)

    // 사이드바 "다가오는 일정" 패널용.
    @GetMapping("/upcoming")
    fun listUpcomingEvents(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @RequestParam(defaultValue = "3") size: Int
    ): List<EventResponse> = eventService.listUpcomingEvents(principal.id, groupId, size.coerceIn(1, 20))

    @GetMapping("/{eventId}")
    fun getEvent(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable eventId: Long
    ): EventDetailResponse = eventService.getEvent(principal.id, groupId, eventId)

    @PatchMapping("/{eventId}")
    fun updateEvent(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable eventId: Long,
        @Valid @RequestBody request: UpdateEventRequest
    ): EventResponse = eventService.updateEvent(principal.id, groupId, eventId, request)

    @DeleteMapping("/{eventId}")
    fun deleteEvent(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable eventId: Long
    ): ResponseEntity<Void> {
        eventService.deleteEvent(principal.id, groupId, eventId)
        return ResponseEntity.noContent().build()
    }

    // RSVP는 일정당 하나라 upsert 의미의 PUT. 응답은 집계가 갱신된 일정으로 통일.
    @PutMapping("/{eventId}/rsvp")
    fun rsvp(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable eventId: Long,
        @Valid @RequestBody request: RsvpRequest
    ): EventResponse = eventService.rsvp(principal.id, groupId, eventId, request.status)

    @DeleteMapping("/{eventId}/rsvp")
    fun cancelRsvp(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable eventId: Long
    ): EventResponse = eventService.cancelRsvp(principal.id, groupId, eventId)
}
