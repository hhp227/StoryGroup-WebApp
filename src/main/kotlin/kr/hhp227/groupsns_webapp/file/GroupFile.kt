package kr.hhp227.groupsns_webapp.file

import java.time.OffsetDateTime

// java.io.File과의 혼동을 피하기 위해 GroupFile로 명명.
data class GroupFile(
    val id: Long,
    val groupId: Long,
    val userId: Long,
    val name: String,
    val url: String,
    val size: Long?,
    val contentType: String?,
    val createdAt: OffsetDateTime,
    val deletedAt: OffsetDateTime?
)

// MyBatis useGeneratedKeys는 결과를 세팅할 mutable 프로퍼티가 필요해 GroupFile(불변)과 분리한 삽입 전용 홀더.
class NewFileRecord(
    val groupId: Long,
    val userId: Long,
    val name: String,
    val url: String,
    val size: Long?,
    val contentType: String?
) {
    var id: Long = 0
}

// 업로더 정보(name/profile_img)를 조인해서 가져오는 조회 전용 row. 목록/단건 조회 공용.
data class FileFeedRow(
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
)
