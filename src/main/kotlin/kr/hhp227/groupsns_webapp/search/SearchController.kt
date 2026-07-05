package kr.hhp227.groupsns_webapp.search

import kr.hhp227.groupsns_webapp.search.dto.SearchResponse
import kr.hhp227.groupsns_webapp.security.UserPrincipal
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

// PRD 13번 "검색": 내가 속한 그룹 범위 안에서 그룹/게시글/파일/메시지를 한 번에 검색한다.
@RestController
@RequestMapping("/api/search")
class SearchController(private val searchService: SearchService) {

    @GetMapping
    fun search(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam query: String,
        @RequestParam(defaultValue = "10") limit: Int
    ): SearchResponse = searchService.search(principal.id, query, limit.coerceIn(1, 20))
}
