package kr.hhp227.groupsns_webapp.block

import kr.hhp227.groupsns_webapp.block.dto.BlockedUserResponse
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
class BlockController(private val blockService: BlockService) {

    @GetMapping("/me/blocks")
    fun listBlockedUsers(@AuthenticationPrincipal principal: UserPrincipal): List<BlockedUserResponse> =
        blockService.listBlockedUsers(principal.id)

    @PostMapping("/{userId}/block")
    fun blockUser(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable userId: Long
    ): ResponseEntity<Void> {
        blockService.blockUser(principal.id, userId)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @DeleteMapping("/{userId}/block")
    fun unblockUser(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable userId: Long
    ): ResponseEntity<Void> {
        blockService.unblockUser(principal.id, userId)
        return ResponseEntity.noContent().build()
    }
}
