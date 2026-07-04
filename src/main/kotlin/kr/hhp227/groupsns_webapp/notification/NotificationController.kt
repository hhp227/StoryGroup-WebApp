package kr.hhp227.groupsns_webapp.notification

import kr.hhp227.groupsns_webapp.notification.dto.NotificationResponse
import kr.hhp227.groupsns_webapp.notification.dto.UnreadCountResponse
import kr.hhp227.groupsns_webapp.security.UserPrincipal
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/notifications")
class NotificationController(private val notificationService: NotificationService) {

    @GetMapping
    fun listNotifications(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): List<NotificationResponse> = notificationService.listNotifications(principal.id, page, size.coerceIn(1, 50))

    @GetMapping("/unread-count")
    fun countUnread(@AuthenticationPrincipal principal: UserPrincipal): UnreadCountResponse =
        notificationService.countUnread(principal.id)

    @PatchMapping("/{notificationId}/read")
    fun markAsRead(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable notificationId: Long
    ): ResponseEntity<Void> {
        notificationService.markAsRead(principal.id, notificationId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/read-all")
    fun markAllAsRead(@AuthenticationPrincipal principal: UserPrincipal): ResponseEntity<Void> {
        notificationService.markAllAsRead(principal.id)
        return ResponseEntity.noContent().build()
    }
}
