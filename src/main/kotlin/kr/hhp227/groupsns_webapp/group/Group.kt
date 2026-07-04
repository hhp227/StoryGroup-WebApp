package kr.hhp227.groupsns_webapp.group

import java.time.OffsetDateTime

data class Group(
    val id: Long,
    val authorId: Long,
    val name: String,
    val image: String?,
    val description: String?,
    val joinType: Int,
    val createdAt: OffsetDateTime,
    val deletedAt: OffsetDateTime?,
    val isLounge: Boolean
)

// MyBatis useGeneratedKeys는 결과를 세팅할 mutable 프로퍼티가 필요해 Group(불변)과 분리한 삽입 전용 홀더.
class NewGroupRecord(
    val authorId: Long,
    val name: String,
    val image: String?,
    val description: String?,
    val joinType: Int
) {
    var id: Long = 0
}

class GroupUpdate(
    val id: Long,
    val name: String,
    val image: String?,
    val description: String?
)
