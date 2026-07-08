package kr.hhp227.groupsns_webapp.post

import kr.hhp227.groupsns_webapp.post.dto.GroupNoticesResponse
import kr.hhp227.groupsns_webapp.security.UserPrincipal
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

// 사이드바 공지 패널용 — 최근 공지 몇 건과 총 개수만 얇게 내려준다.
// 공지 지정/해제는 기존 PostController(/posts/{id}/notice)가 담당.
@RestController
@RequestMapping("/api/groups/{groupId}/notices")
class GroupNoticeController(private val postService: PostService) {

    @GetMapping
    fun listNotices(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @RequestParam(defaultValue = "3") size: Int
    ): GroupNoticesResponse = postService.listNotices(principal.id, groupId, size.coerceIn(1, 20))
}
