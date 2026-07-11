package kr.hhp227.groupsns_webapp.friend

import kr.hhp227.groupsns_webapp.friend.dto.FriendResponse
import kr.hhp227.groupsns_webapp.security.UserPrincipal
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class FriendController(private val friendService: FriendService) {

    @GetMapping("/me/friends")
    fun listFriends(@AuthenticationPrincipal principal: UserPrincipal): List<FriendResponse> =
        friendService.listFriends(principal.id)

    @PostMapping("/{userId}/friend")
    fun addFriend(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable userId: Long
    ): ResponseEntity<Void> {
        friendService.addFriend(principal.id, userId)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @DeleteMapping("/{userId}/friend")
    fun removeFriend(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable userId: Long
    ): ResponseEntity<Void> {
        friendService.removeFriend(principal.id, userId)
        return ResponseEntity.noContent().build()
    }
}
