package kr.hhp227.groupsns_webapp.post

import kr.hhp227.groupsns_webapp.post.dto.CreatePostRequest
import kr.hhp227.groupsns_webapp.post.dto.PostResponse
import kr.hhp227.groupsns_webapp.post.dto.UpdatePostRequest
import kr.hhp227.groupsns_webapp.security.UserPrincipal
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@RequestMapping("/api/groups/{groupId}/posts")
class PostController(private val postService: PostService) {

    @PostMapping
    fun createPost(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @Valid @RequestBody request: CreatePostRequest
    ): ResponseEntity<PostResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(postService.createPost(principal.id, groupId, request))

    @GetMapping
    fun listPosts(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): List<PostResponse> = postService.listPosts(principal.id, groupId, page, size.coerceIn(1, 50))

    @GetMapping("/{postId}")
    fun getPost(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable postId: Long
    ): PostResponse = postService.getPost(principal.id, groupId, postId)

    @PatchMapping("/{postId}")
    fun updatePost(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable postId: Long,
        @Valid @RequestBody request: UpdatePostRequest
    ): PostResponse = postService.updatePost(principal.id, groupId, postId, request)

    @DeleteMapping("/{postId}")
    fun deletePost(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable postId: Long
    ): ResponseEntity<Void> {
        postService.deletePost(principal.id, groupId, postId)
        return ResponseEntity.noContent().build()
    }
}
