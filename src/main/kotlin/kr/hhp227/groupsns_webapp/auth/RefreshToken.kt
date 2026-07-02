package kr.hhp227.groupsns_webapp.auth

import java.time.OffsetDateTime

data class RefreshToken(
    val id: Long,
    val userId: Long,
    val tokenHash: String,
    val deviceInfo: String?,
    val expiresAt: OffsetDateTime,
    val revokedAt: OffsetDateTime?,
    val createdAt: OffsetDateTime
) {
    fun isUsable(now: OffsetDateTime): Boolean = revokedAt == null && expiresAt.isAfter(now)
}

// MyBatis useGeneratedKeys는 결과를 세팅할 mutable 프로퍼티가 필요해 RefreshToken(불변)과 분리한 삽입 전용 홀더.
class NewRefreshTokenRecord(
    val userId: Long,
    val tokenHash: String,
    val deviceInfo: String?,
    val expiresAt: OffsetDateTime
) {
    var id: Long = 0
}
