package kr.hhp227.groupsns_webapp.like

import kr.hhp227.groupsns_webapp.like.dto.LikeResponse
import kr.hhp227.groupsns_webapp.security.UserPrincipal
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/groups/{groupId}/posts/{postId}/likes")
class LikeController(private val likeService: LikeService) {

    @PostMapping
    fun likePost(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable postId: Long
    ): ResponseEntity<Void> {
        likeService.likePost(principal.id, groupId, postId)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @DeleteMapping
    fun unlikePost(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable postId: Long
    ): ResponseEntity<Void> {
        likeService.unlikePost(principal.id, groupId, postId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping
    fun listLikes(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable postId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): List<LikeResponse> = likeService.listLikes(principal.id, groupId, postId, page, size.coerceIn(1, 50))
}
