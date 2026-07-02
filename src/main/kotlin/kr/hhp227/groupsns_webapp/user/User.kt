package kr.hhp227.groupsns_webapp.user

import java.time.OffsetDateTime

data class User(
    val id: Long,
    val name: String,
    val email: String,
    val passwordHash: String?,
    val status: Int,
    val profileImg: String?,
    val fcmRegistrationId: String?,
    val createdAt: OffsetDateTime,
    val deletedAt: OffsetDateTime?
)

// MyBatis useGeneratedKeys는 결과를 세팅할 mutable 프로퍼티가 필요해 User(불변)와 분리한 삽입 전용 홀더.
class NewUserRecord(
    val name: String,
    val email: String,
    val passwordHash: String
) {
    var id: Long = 0
}
