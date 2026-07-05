package kr.hhp227.groupsns_webapp.search.dto

import kr.hhp227.groupsns_webapp.search.FileSearchRow
import kr.hhp227.groupsns_webapp.search.GroupSearchRow
import kr.hhp227.groupsns_webapp.search.MessageSearchRow
import kr.hhp227.groupsns_webapp.search.PostSearchRow
import java.time.OffsetDateTime

data class GroupSearchResult(
    val id: Long,
    val name: String,
    val image: String?,
    val description: String?
) {
    companion object {
        fun from(row: GroupSearchRow) = GroupSearchResult(row.id, row.name, row.image, row.description)
    }
}

data class PostSearchResult(
    val id: Long,
    val groupId: Long,
    val groupName: String,
    val authorName: String,
    val text: String,
    val createdAt: OffsetDateTime
) {
    companion object {
        fun from(row: PostSearchRow) =
            PostSearchResult(row.id, row.groupId, row.groupName, row.authorName, row.text, row.createdAt)
    }
}

data class FileSearchResult(
    val id: Long,
    val groupId: Long,
    val groupName: String,
    val name: String,
    val url: String,
    val createdAt: OffsetDateTime
) {
    companion object {
        fun from(row: FileSearchRow) =
            FileSearchResult(row.id, row.groupId, row.groupName, row.name, row.url, row.createdAt)
    }
}

data class MessageSearchResult(
    val id: Long,
    val chatRoomId: Long,
    val groupId: Long?,
    val groupName: String?,
    val authorName: String,
    val text: String,
    val createdAt: OffsetDateTime
) {
    companion object {
        fun from(row: MessageSearchRow) =
            MessageSearchResult(row.id, row.chatRoomId, row.groupId, row.groupName, row.authorName, row.message, row.createdAt)
    }
}

data class SearchResponse(
    val groups: List<GroupSearchResult>,
    val posts: List<PostSearchResult>,
    val files: List<FileSearchResult>,
    val messages: List<MessageSearchResult>
)
