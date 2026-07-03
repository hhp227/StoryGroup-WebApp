package kr.hhp227.groupsns_webapp.post

import java.time.OffsetDateTime

data class Post(
    val id: Long,
    val groupId: Long,
    val userId: Long,
    val text: String,
    val createdAt: OffsetDateTime,
    val deletedAt: OffsetDateTime?
)

// MyBatis useGeneratedKeys는 결과를 세팅할 mutable 프로퍼티가 필요해 Post(불변)와 분리한 삽입 전용 홀더.
class NewPostRecord(
    val groupId: Long,
    val userId: Long,
    val text: String
) {
    var id: Long = 0
}

class PostUpdate(
    val id: Long,
    val text: String
)

// 작성자 정보(name/profile_img)를 조인해서 가져오는 조회 전용 row. 목록/단건 조회 공용.
data class PostFeedRow(
    val id: Long,
    val groupId: Long,
    val userId: Long,
    val authorName: String,
    val authorProfileImg: String?,
    val text: String,
    val createdAt: OffsetDateTime
)
