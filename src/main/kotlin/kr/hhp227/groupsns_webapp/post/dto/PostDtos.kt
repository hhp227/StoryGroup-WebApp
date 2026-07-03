package kr.hhp227.groupsns_webapp.post.dto

import kr.hhp227.groupsns_webapp.post.Image
import kr.hhp227.groupsns_webapp.post.PostFeedRow
import java.time.OffsetDateTime
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

data class CreatePostRequest(
    @field:NotBlank val text: String,
    val images: List<@Size(max = 255) String>? = null
)

data class UpdatePostRequest(
    @field:NotBlank val text: String,
    // null: 이미지 목록 변경 없음, 빈 리스트: 전체 삭제, 값 있음: 전체 교체
    val images: List<@Size(max = 255) String>? = null
)

data class ImageResponse(
    val id: Long,
    val image: String
) {
    companion object {
        fun from(image: Image) = ImageResponse(image.id, image.image)
    }
}

data class PostResponse(
    val id: Long,
    val groupId: Long,
    val userId: Long,
    val authorName: String,
    val authorProfileImg: String?,
    val text: String,
    val images: List<ImageResponse>,
    val createdAt: OffsetDateTime
) {
    companion object {
        fun from(row: PostFeedRow, images: List<Image>) = PostResponse(
            id = row.id,
            groupId = row.groupId,
            userId = row.userId,
            authorName = row.authorName,
            authorProfileImg = row.authorProfileImg,
            text = row.text,
            images = images.map { ImageResponse.from(it) },
            createdAt = row.createdAt
        )
    }
}
