package kr.hhp227.groupsns_webapp.comment

import kr.hhp227.groupsns_webapp.comment.dto.CommentResponse
import kr.hhp227.groupsns_webapp.comment.dto.CreateCommentRequest
import kr.hhp227.groupsns_webapp.comment.dto.UpdateCommentRequest
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
@RequestMapping("/api/groups/{groupId}/posts/{postId}/comments")
class CommentController(private val commentService: CommentService) {

    @PostMapping
    fun createComment(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable postId: Long,
        @Valid @RequestBody request: CreateCommentRequest
    ): ResponseEntity<CommentResponse> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(commentService.createComment(principal.id, groupId, postId, request))

    @GetMapping
    fun listComments(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable postId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): List<CommentResponse> = commentService.listComments(principal.id, groupId, postId, page, size.coerceIn(1, 50))

    @GetMapping("/{commentId}")
    fun getComment(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable postId: Long,
        @PathVariable commentId: Long
    ): CommentResponse = commentService.getComment(principal.id, groupId, postId, commentId)

    @PatchMapping("/{commentId}")
    fun updateComment(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable postId: Long,
        @PathVariable commentId: Long,
        @Valid @RequestBody request: UpdateCommentRequest
    ): CommentResponse = commentService.updateComment(principal.id, groupId, postId, commentId, request)

    @DeleteMapping("/{commentId}")
    fun deleteComment(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable postId: Long,
        @PathVariable commentId: Long
    ): ResponseEntity<Void> {
        commentService.deleteComment(principal.id, groupId, postId, commentId)
        return ResponseEntity.noContent().build()
    }
}
