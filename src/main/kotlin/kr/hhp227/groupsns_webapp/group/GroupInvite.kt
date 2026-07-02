package kr.hhp227.groupsns_webapp.group

import java.time.OffsetDateTime

data class GroupInvite(
    val id: Long,
    val groupId: Long,
    val code: String,
    val createdBy: Long,
    val maxUses: Int?,
    val usedCount: Int,
    val expiresAt: OffsetDateTime?,
    val createdAt: OffsetDateTime
) {
    fun isUsable(now: OffsetDateTime): Boolean {
        if (expiresAt != null && expiresAt.isBefore(now)) return false
        if (maxUses != null && usedCount >= maxUses) return false
        return true
    }
}

// MyBatis useGeneratedKeys는 결과를 세팅할 mutable 프로퍼티가 필요해 GroupInvite(불변)와 분리한 삽입 전용 홀더.
class NewGroupInviteRecord(
    val groupId: Long,
    val code: String,
    val createdBy: Long,
    val maxUses: Int?,
    val expiresAt: OffsetDateTime?
) {
    var id: Long = 0
}
