package kr.hhp227.groupsns_webapp.file.dto

import kr.hhp227.groupsns_webapp.file.FileFeedRow
import java.time.OffsetDateTime
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Positive
import javax.validation.constraints.Size

data class CreateFileRequest(
    @field:NotBlank @field:Size(max = 255) val name: String,
    @field:NotBlank @field:Size(max = 500) val url: String,
    @field:Positive val size: Long? = null,
    @field:Size(max = 100) val contentType: String? = null
)

data class FileResponse(
    val id: Long,
    val groupId: Long,
    val userId: Long,
    val authorName: String,
    val authorProfileImg: String?,
    val name: String,
    val url: String,
    val size: Long?,
    val contentType: String?,
    val createdAt: OffsetDateTime
) {
    companion object {
        fun from(row: FileFeedRow) = FileResponse(
            id = row.id,
            groupId = row.groupId,
            userId = row.userId,
            authorName = row.authorName,
            authorProfileImg = row.authorProfileImg,
            name = row.name,
            url = row.url,
            size = row.size,
            contentType = row.contentType,
            createdAt = row.createdAt
        )
    }
}
