package kr.hhp227.groupsns_webapp.block.dto

import kr.hhp227.groupsns_webapp.block.BlockedUserRow
import java.time.OffsetDateTime

data class BlockedUserResponse(
    val userId: Long,
    val name: String,
    val profileImg: String?,
    val blockedAt: OffsetDateTime
) {
    companion object {
        fun from(row: BlockedUserRow) = BlockedUserResponse(row.userId, row.name, row.profileImg, row.blockedAt)
    }
}
