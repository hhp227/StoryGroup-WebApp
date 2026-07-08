package kr.hhp227.groupsns_webapp.post

import kr.hhp227.groupsns_webapp.post.dto.GroupPhotosResponse
import kr.hhp227.groupsns_webapp.security.UserPrincipal
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

// 그룹 앨범(파생 뷰) — 별도 앨범 엔티티 없이 그룹 게시글의 첨부 이미지를 모아 내려준다.
// 라운지도 is_lounge 그룹이라 홈 앨범 패널과 그룹 메인 패널이 이 엔드포인트 하나를 같이 쓴다.
@RestController
@RequestMapping("/api/groups/{groupId}/photos")
class GroupPhotoController(private val postService: PostService) {

    @GetMapping
    fun listPhotos(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "30") size: Int
    ): GroupPhotosResponse = postService.listPhotos(principal.id, groupId, page, size.coerceIn(1, 60))
}
