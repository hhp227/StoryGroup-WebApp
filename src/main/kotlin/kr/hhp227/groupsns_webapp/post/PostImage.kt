package kr.hhp227.groupsns_webapp.post

import java.time.OffsetDateTime

data class Image(
    val id: Long,
    val postId: Long,
    val image: String
)

// MyBatis useGeneratedKeys는 결과를 세팅할 mutable 프로퍼티가 필요해 Image(불변)와 분리한 삽입 전용 홀더.
class NewImageRecord(
    val postId: Long,
    val userId: Long,
    val image: String
) {
    var id: Long = 0
}

// 그룹 앨범(파생 뷰) 조회 전용 row — 게시글 작성자/작성일을 함께 조인한다.
data class GroupPhotoRow(
    val id: Long,
    val postId: Long,
    val image: String,
    val userId: Long,
    val authorName: String,
    val createdAt: OffsetDateTime
)
