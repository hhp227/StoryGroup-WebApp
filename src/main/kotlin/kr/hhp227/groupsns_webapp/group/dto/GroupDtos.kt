package kr.hhp227.groupsns_webapp.group.dto

import kr.hhp227.groupsns_webapp.group.Group
import kr.hhp227.groupsns_webapp.group.GroupInvite
import kr.hhp227.groupsns_webapp.group.GroupMemberRow
import kr.hhp227.groupsns_webapp.group.GroupRole
import kr.hhp227.groupsns_webapp.group.GroupWithRoleRow
import java.time.OffsetDateTime
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

// PRD 7번 섹션 "공개 여부": 비공개/초대 전용. 현재는 두 값 모두 "초대 코드로만 가입 가능"으로 동작하고
// join_type은 추후 그룹 검색 노출 범위 등에 쓸 메타데이터로만 우선 저장한다.
enum class GroupJoinType(val code: Int) {
    PRIVATE(0), INVITE_ONLY(1);

    companion object {
        fun fromCode(code: Int): GroupJoinType = values().first { it.code == code }
    }
}

data class CreateGroupRequest(
    @field:NotBlank @field:Size(max = 100) val name: String,
    @field:Size(max = 1000) val description: String?,
    @field:Size(max = 255) val image: String?,
    val joinType: GroupJoinType = GroupJoinType.PRIVATE
)

data class UpdateGroupRequest(
    @field:NotBlank @field:Size(max = 100) val name: String,
    @field:Size(max = 1000) val description: String?,
    @field:Size(max = 255) val image: String?
)

data class GroupResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val image: String?,
    val joinType: GroupJoinType,
    val myRole: GroupRole,
    val createdAt: OffsetDateTime,
    val isLounge: Boolean
) {
    companion object {
        fun from(group: Group, myRole: GroupRole) = GroupResponse(
            id = group.id,
            name = group.name,
            description = group.description,
            image = group.image,
            joinType = GroupJoinType.fromCode(group.joinType),
            myRole = myRole,
            createdAt = group.createdAt,
            isLounge = group.isLounge
        )

        fun from(row: GroupWithRoleRow) = GroupResponse(
            id = row.id,
            name = row.name,
            description = row.description,
            image = row.image,
            joinType = GroupJoinType.fromCode(row.joinType),
            myRole = row.myRole,
            createdAt = row.createdAt,
            isLounge = row.isLounge
        )
    }
}

data class UpdateMemberRoleRequest(
    val role: GroupRole
)

data class CreateInviteRequest(
    @field:Min(1) val maxUses: Int? = null,
    @field:Min(1) @field:Max(365) val expiresInDays: Int? = null
)

data class InviteResponse(
    val code: String,
    val maxUses: Int?,
    val expiresAt: OffsetDateTime?
) {
    companion object {
        fun from(invite: GroupInvite) = InviteResponse(invite.code, invite.maxUses, invite.expiresAt)
    }
}

data class MemberResponse(
    val userId: Long,
    val name: String,
    val profileImg: String?,
    val role: GroupRole,
    val joinedAt: OffsetDateTime
) {
    companion object {
        fun from(row: GroupMemberRow) = MemberResponse(row.userId, row.name, row.profileImg, row.role, row.joinedAt)
    }
}
