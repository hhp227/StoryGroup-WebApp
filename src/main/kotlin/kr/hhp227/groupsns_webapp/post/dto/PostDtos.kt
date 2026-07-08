package kr.hhp227.groupsns_webapp.post.dto

import kr.hhp227.groupsns_webapp.post.GroupPhotoRow
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

// 그룹 앨범(파생 뷰) 한 장 — 클릭 시 원본 게시글로 이동할 수 있게 postId를 함께 준다.
data class GroupPhotoResponse(
    val id: Long,
    val postId: Long,
    val image: String,
    val userId: Long,
    val authorName: String,
    val createdAt: OffsetDateTime
) {
    companion object {
        fun from(row: GroupPhotoRow) = GroupPhotoResponse(
            id = row.id,
            postId = row.postId,
            image = row.image,
            userId = row.userId,
            authorName = row.authorName,
            createdAt = row.createdAt
        )
    }
}

// 목록형 응답들과 달리 총 개수를 함께 준다 — 사이드바 앨범 패널의 "+N"/"N장" 표기에 필요.
data class GroupPhotosResponse(
    val totalCount: Long,
    val photos: List<GroupPhotoResponse>
)

data class PostResponse(
    val id: Long,
    val groupId: Long,
    val userId: Long,
    val authorName: String,
    val authorProfileImg: String?,
    val text: String,
    val images: List<ImageResponse>,
    val isNotice: Boolean,
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
            isNotice = row.isNotice,
            createdAt = row.createdAt
        )
    }
}
