package kr.hhp227.groupsns_webapp.friend.dto

import kr.hhp227.groupsns_webapp.friend.FriendRow
import java.time.OffsetDateTime

data class FriendResponse(
    val userId: Long,
    val name: String,
    val profileImg: String?,
    val statusMessage: String?,
    val friendedAt: OffsetDateTime,
    // 전역 프레즌스 스냅샷(인메모리 트래커 조회) — 실시간 전환은 개인 큐 PRESENCE_CHANGED가 증분으로 나간다
    val online: Boolean
) {
    companion object {
        fun from(row: FriendRow, online: Boolean) = FriendResponse(
            userId = row.userId,
            name = row.name,
            profileImg = row.profileImg,
            statusMessage = row.statusMessage,
            friendedAt = row.friendedAt,
            online = online
        )
    }
}
