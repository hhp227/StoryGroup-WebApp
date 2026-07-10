package kr.hhp227.groupsns_webapp.group

import java.time.OffsetDateTime

enum class GroupRole {
    OWNER, ADMIN, MEMBER;

    // 방장(OWNER)/부방장(ADMIN) 공통의 조정 권한 - 남의 게시글/댓글 삭제, 공지 지정, 초대, 강퇴.
    val isModerator: Boolean
        get() = this != MEMBER
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

// 그룹 탐색 목록 한 행 - 멤버 수와 "나와의 관계"(가입됨/신청 대기)를 함께 조회한다.
data class DiscoverGroupRow(
    val id: Long,
    val name: String,
    val image: String?,
    val description: String?,
    val joinType: Int,
    val createdAt: OffsetDateTime,
    val memberCount: Long,
    val isMember: Boolean,
    val isPending: Boolean
)

// 가입 신청 목록 한 행(모더레이터 승인 화면용) - 신청자 프로필을 함께 조회한다.
data class GroupJoinRequestRow(
    val userId: Long,
    val name: String,
    val profileImg: String?,
    val createdAt: OffsetDateTime
)
