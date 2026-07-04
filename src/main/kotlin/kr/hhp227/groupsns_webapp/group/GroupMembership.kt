package kr.hhp227.groupsns_webapp.group

import java.time.OffsetDateTime

enum class GroupRole {
    OWNER, MEMBER
}

data class GroupWithRoleRow(
    val id: Long,
    val authorId: Long,
    val name: String,
    val image: String?,
    val description: String?,
    val joinType: Int,
    val createdAt: OffsetDateTime,
    val deletedAt: OffsetDateTime?,
    val myRole: GroupRole,
    val isLounge: Boolean
)

data class GroupMemberRow(
    val userId: Long,
    val name: String,
    val profileImg: String?,
    val role: GroupRole,
    val joinedAt: OffsetDateTime
)
