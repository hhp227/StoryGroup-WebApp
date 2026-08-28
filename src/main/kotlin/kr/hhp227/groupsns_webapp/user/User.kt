package kr.hhp227.groupsns_webapp.user

import java.time.OffsetDateTime

// 앱 운영자 등급 - 그룹 역할(GroupRole)과 무관한 앱 전체 권한(사용자 신고 관리 등).
enum class UserRole {
    USER, ADMIN
}

data class User(
    val id: Long,
    val name: String,
    val email: String,
    val passwordHash: String?,
    val status: Int,
    val role: UserRole,
    val profileImg: String?,
    val bio: String?,
    val statusMessage: String?,
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

class UserProfileUpdate(
    val id: Long,
    val name: String,
    val profileImg: String?,
    val bio: String?,
    val statusMessage: String?
)
