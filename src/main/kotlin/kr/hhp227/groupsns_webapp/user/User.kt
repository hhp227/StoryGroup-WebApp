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

// 푸시 종류별 on/off 행(설계 §1) — 발송 게이트(PushBroadcaster)와 설정 API만 읽는다.
// User에 필드를 얹지 않는다: 소비처가 이 둘뿐이고, User SELECT 2곳·테스트 빌더까지 건드릴 이유가 없다.
// MyBatis는 단일 생성자를 SELECT 컬럼 순서대로 채우므로 매퍼의 컬럼 순서 = (chat, activity)를 지킬 것.
data class PushPreferences(
    val chatEnabled: Boolean,
    val activityEnabled: Boolean
)
