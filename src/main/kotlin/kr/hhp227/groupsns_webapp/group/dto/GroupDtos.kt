package kr.hhp227.groupsns_webapp.group.dto

import kr.hhp227.groupsns_webapp.group.DiscoverGroupRow
import kr.hhp227.groupsns_webapp.group.Group
import kr.hhp227.groupsns_webapp.group.GroupInvite
import kr.hhp227.groupsns_webapp.group.GroupJoinRequestRow
import kr.hhp227.groupsns_webapp.group.GroupMemberRow
import kr.hhp227.groupsns_webapp.group.GroupRole
import kr.hhp227.groupsns_webapp.group.GroupWithRoleRow
import java.time.OffsetDateTime
import javax.validation.constraints.Max
import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

// 레거시 join_type 의미 복원: 0=자동 승인(그룹 탐색에서 바로 가입), 1=승인제(가입 신청 후
// 방장/부방장 승인). 모든 그룹이 탐색에 노출되며, 초대 코드 가입은 두 값 모두에서 계속 동작한다.
// (초기에는 PRIVATE/INVITE_ONLY라는 이름의 메타데이터로만 저장했으나, 코드값 0/1은 레거시
//  데이터와 동일해 마이그레이션 없이 이름만 바로잡았다.)
enum class GroupJoinType(val code: Int) {
    AUTO_APPROVE(0), APPROVAL_REQUIRED(1);

    companion object {
        fun fromCode(code: Int): GroupJoinType = values().first { it.code == code }
    }
}

data class CreateGroupRequest(
    @field:NotBlank @field:Size(max = 100) val name: String,
    @field:Size(max = 1000) val description: String?,
    @field:Size(max = 255) val image: String?,
    val joinType: GroupJoinType = GroupJoinType.AUTO_APPROVE
)

data class UpdateGroupRequest(
    @field:NotBlank @field:Size(max = 100) val name: String,
    @field:Size(max = 1000) val description: String?,
    @field:Size(max = 255) val image: String?,
    // null이면 기존 값 유지 - joinType을 모르는 배포 전 웹 클라이언트가 설정을 초기화하지 않도록.
    val joinType: GroupJoinType? = null
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

// 그룹 탐색 화면에서 본 그룹과 나의 관계. MEMBER는 역할과 무관하게 "이미 가입됨"을 뜻한다.
enum class MembershipStatus { NONE, PENDING, MEMBER }

data class DiscoverGroupResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val image: String?,
    val joinType: GroupJoinType,
    val memberCount: Long,
    val membership: MembershipStatus,
    val createdAt: OffsetDateTime
) {
    companion object {
        fun from(row: DiscoverGroupRow) = DiscoverGroupResponse(
            id = row.id,
            name = row.name,
            description = row.description,
            image = row.image,
            joinType = GroupJoinType.fromCode(row.joinType),
            memberCount = row.memberCount,
            membership = when {
                row.isMember -> MembershipStatus.MEMBER
                row.isPending -> MembershipStatus.PENDING
                else -> MembershipStatus.NONE
            },
            createdAt = row.createdAt
        )
    }
}

enum class JoinResult { JOINED, REQUESTED }

// 자동 승인 그룹이면 JOINED + 가입된 그룹 정보, 승인제면 REQUESTED + group=null.
data class JoinGroupResponse(
    val status: JoinResult,
    val group: GroupResponse?
)

data class JoinRequestResponse(
    val userId: Long,
    val name: String,
    val profileImg: String?,
    val requestedAt: OffsetDateTime
) {
    companion object {
        fun from(row: GroupJoinRequestRow) = JoinRequestResponse(row.userId, row.name, row.profileImg, row.createdAt)
    }
}
