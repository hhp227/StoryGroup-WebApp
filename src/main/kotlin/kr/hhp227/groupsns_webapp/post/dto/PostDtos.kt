package kr.hhp227.groupsns_webapp.post.dto

import kr.hhp227.groupsns_webapp.post.GroupPhotoRow
import kr.hhp227.groupsns_webapp.post.Image
import kr.hhp227.groupsns_webapp.post.MEDIA_TYPE_VIDEO
import kr.hhp227.groupsns_webapp.post.PostFeedRow
import java.time.OffsetDateTime
import javax.validation.constraints.Size

// text에 @NotBlank가 없는 것은 첨부만 있는 게시글을 허용하기 위함 —
// "본문/첨부 중 하나는 필수" 검증은 첨부 상태를 알아야 해서 PostService에서 한다.
data class CreatePostRequest(
    val text: String,
    val images: List<@Size(max = 255) String>? = null,
    val videos: List<@Size(max = 255) String>? = null
)

data class UpdatePostRequest(
    val text: String,
    // null: 목록 변경 없음, 빈 리스트: 전체 삭제, 값 있음: 전체 교체 — images/videos 각각 독립 적용
    val images: List<@Size(max = 255) String>? = null,
    val videos: List<@Size(max = 255) String>? = null
)

data class ImageResponse(
    val id: Long,
    val image: String
) {
    companion object {
        fun from(image: Image) = ImageResponse(image.id, image.image)
    }
}

data class VideoResponse(
    val id: Long,
    val video: String
) {
    companion object {
        fun from(image: Image) = VideoResponse(image.id, image.image)
    }
}

// 그룹 앨범(파생 뷰) 한 장 — 클릭 시 원본 게시글로 이동할 수 있게 postId를 함께 준다.
// mediaType('image'|'video')으로 클라이언트가 <img>/<video> 썸네일 렌더링을 가른다.
data class GroupPhotoResponse(
    val id: Long,
    val postId: Long,
    val image: String,
    val mediaType: String,
    val userId: Long,
    val authorName: String,
    val createdAt: OffsetDateTime
) {
    companion object {
        fun from(row: GroupPhotoRow) = GroupPhotoResponse(
            id = row.id,
            postId = row.postId,
            image = row.image,
            mediaType = row.mediaType,
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

// 사이드바 공지 패널 한 줄 — 본문 요약만 필요해서 PostResponse보다 얇다(이미지 조인 없음).
data class NoticeSummaryResponse(
    val id: Long,
    val text: String,
    val authorName: String,
    val createdAt: OffsetDateTime
) {
    companion object {
        fun from(row: PostFeedRow) = NoticeSummaryResponse(row.id, row.text, row.authorName, row.createdAt)
    }
}

data class GroupNoticesResponse(
    val totalCount: Long,
    val notices: List<NoticeSummaryResponse>
)

data class PostResponse(
    val id: Long,
    val groupId: Long,
    val userId: Long,
    val authorName: String,
    val authorProfileImg: String?,
    val text: String,
    val images: List<ImageResponse>,
    val videos: List<VideoResponse>,
    val isNotice: Boolean,
    val createdAt: OffsetDateTime,
    val likeCount: Int,
    val replyCount: Int,
    val likedByMe: Boolean
) {
    companion object {
        // attachments는 images 테이블의 이미지+동영상 혼합 목록 — media_type으로 갈라서 내려준다.
        // images를 그대로 두고 videos를 추가한 것은 기존 클라이언트(images만 아는)와의 하위호환 때문.
        fun from(row: PostFeedRow, attachments: List<Image>) = PostResponse(
            id = row.id,
            groupId = row.groupId,
            userId = row.userId,
            authorName = row.authorName,
            authorProfileImg = row.authorProfileImg,
            text = row.text,
            images = attachments.filter { it.mediaType != MEDIA_TYPE_VIDEO }.map { ImageResponse.from(it) },
            videos = attachments.filter { it.mediaType == MEDIA_TYPE_VIDEO }.map { VideoResponse.from(it) },
            isNotice = row.isNotice,
            createdAt = row.createdAt,
            likeCount = row.likeCount,
            replyCount = row.replyCount,
            likedByMe = row.likedByMe
        )
    }
}
