package kr.hhp227.groupsns_webapp.search

import kr.hhp227.groupsns_webapp.common.db.DbSessionMapper
import kr.hhp227.groupsns_webapp.search.dto.FileSearchResult
import kr.hhp227.groupsns_webapp.search.dto.GroupSearchResult
import kr.hhp227.groupsns_webapp.search.dto.MessageSearchResult
import kr.hhp227.groupsns_webapp.search.dto.PostSearchResult
import kr.hhp227.groupsns_webapp.search.dto.SearchResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SearchService(
    private val searchMapper: SearchMapper,
    private val dbSessionMapper: DbSessionMapper
) {
    @Transactional
    fun search(userId: Long, rawQuery: String, limit: Int): SearchResponse {
        val trimmed = rawQuery.trim()
        if (trimmed.isEmpty()) throw IllegalArgumentException("검색어를 입력해주세요")

        dbSessionMapper.setCurrentUserId(userId)
        val query = escapeLikeWildcards(trimmed)
        return SearchResponse(
            groups = searchMapper.searchGroups(userId, query, limit).map { GroupSearchResult.from(it) },
            posts = searchMapper.searchPosts(userId, query, limit).map { PostSearchResult.from(it) },
            files = searchMapper.searchFiles(userId, query, limit).map { FileSearchResult.from(it) },
            messages = searchMapper.searchMessages(userId, query, limit).map { MessageSearchResult.from(it) }
        )
    }

    // 사용자가 입력한 %/_가 LIKE 와일드카드로 동작하지 않도록 이스케이프한다.
    // (PostgreSQL의 LIKE/ILIKE 기본 이스케이프 문자는 백슬래시)
    private fun escapeLikeWildcards(query: String): String =
        query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
