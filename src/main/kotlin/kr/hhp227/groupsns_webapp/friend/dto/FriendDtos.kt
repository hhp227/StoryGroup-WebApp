package kr.hhp227.groupsns_webapp.friend.dto

import kr.hhp227.groupsns_webapp.friend.FriendRow
import java.time.OffsetDateTime

data class FriendResponse(
    val userId: Long,
    val name: String,
    val profileImg: String?,
    val statusMessage: String?,
    val friendedAt: OffsetDateTime
) {
    companion object {
        fun from(row: FriendRow) = FriendResponse(
            userId = row.userId,
            name = row.name,
            profileImg = row.profileImg,
            statusMessage = row.statusMessage,
            friendedAt = row.friendedAt
        )
    }
}
