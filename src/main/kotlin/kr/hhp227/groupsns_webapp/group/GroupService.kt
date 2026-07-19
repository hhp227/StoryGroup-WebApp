package kr.hhp227.groupsns_webapp.group

import kr.hhp227.groupsns_webapp.chat.ChatRoomMapper
import kr.hhp227.groupsns_webapp.chat.NewChatRoomRecord
import kr.hhp227.groupsns_webapp.common.db.DbSessionMapper
import kr.hhp227.groupsns_webapp.common.exception.AlreadyMemberException
import kr.hhp227.groupsns_webapp.common.exception.AlreadyRequestedException
import kr.hhp227.groupsns_webapp.common.exception.ForbiddenException
import kr.hhp227.groupsns_webapp.common.exception.GroupMemberNotFoundException
import kr.hhp227.groupsns_webapp.common.exception.GroupNotFoundException
import kr.hhp227.groupsns_webapp.common.exception.InvalidInviteException
import kr.hhp227.groupsns_webapp.common.exception.JoinRequestNotFoundException
import kr.hhp227.groupsns_webapp.group.dto.CreateGroupRequest
import kr.hhp227.groupsns_webapp.group.dto.CreateInviteRequest
import kr.hhp227.groupsns_webapp.group.dto.DiscoverGroupResponse
import kr.hhp227.groupsns_webapp.group.dto.GroupJoinType
import kr.hhp227.groupsns_webapp.group.dto.GroupResponse
import kr.hhp227.groupsns_webapp.group.dto.InviteResponse
import kr.hhp227.groupsns_webapp.group.dto.JoinGroupResponse
import kr.hhp227.groupsns_webapp.group.dto.JoinRequestResponse
import kr.hhp227.groupsns_webapp.group.dto.JoinResult
import kr.hhp227.groupsns_webapp.group.dto.MemberResponse
import kr.hhp227.groupsns_webapp.group.dto.UpdateGroupRequest
import kr.hhp227.groupsns_webapp.notification.NotificationService
import kr.hhp227.groupsns_webapp.notification.NotificationTargetType
import kr.hhp227.groupsns_webapp.notification.NotificationType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.OffsetDateTime

@Service
class GroupService(
    private val groupMapper: GroupMapper,
    private val userGroupMapper: UserGroupMapper,
    private val groupInviteMapper: GroupInviteMapper,
    private val groupJoinRequestMapper: GroupJoinRequestMapper,
    private val chatRoomMapper: ChatRoomMapper,
    private val dbSessionMapper: DbSessionMapper,
    private val notificationService: NotificationService
) {
    private val secureRandom = SecureRandom()

    // 혼동되는 0/O, 1/I를 뺀 초대 코드용 문자셋
    private val inviteCodeChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    @Transactional
    fun createGroup(authorId: Long, request: CreateGroupRequest): GroupResponse {
        val record = NewGroupRecord(
            authorId = authorId,
            name = request.name,
            image = request.image,
            description = request.description,
            joinType = request.joinType.code
        )
        groupMapper.insert(record)
        userGroupMapper.insert(authorId, record.id, GroupRole.OWNER)
        // 그룹마다 기본 채팅방("일반")을 하나 자동으로 만들어둔다 — 멤버는 여기에 더해 서브
        // 채팅방을 얼마든지 추가로 만들 수 있다(ChatController.createChatRoom).
        chatRoomMapper.insert(NewChatRoomRecord(record.id, "일반"))
        val group = groupMapper.findById(record.id) ?: throw IllegalStateException("방금 생성한 그룹을 찾을 수 없습니다")
        return GroupResponse.from(group, GroupRole.OWNER)
    }

    @Transactional
    fun listMyGroups(userId: Long, page: Int? = null, size: Int? = null): List<GroupResponse> {
        dbSessionMapper.setCurrentUserId(userId)
        // page/size 둘 다 있어야 페이징 — 하나만 오면 전체 반환(기존 계약)으로 취급
        val rows = if (page != null && size != null) {
            val limit = size.coerceIn(1, 50)

            userGroupMapper.findGroupsForUserPaged(userId, limit, page.coerceAtLeast(0) * limit)
        } else {
            userGroupMapper.findGroupsForUser(userId)
        }
        return rows.map { GroupResponse.from(it) }
    }

    @Transactional
    fun getGroup(userId: Long, groupId: Long): GroupResponse {
        dbSessionMapper.setCurrentUserId(userId)
        val role = requireMembership(userId, groupId)
        val group = groupMapper.findById(groupId) ?: throw GroupNotFoundException()
        return GroupResponse.from(group, role)
    }

    @Transactional
    fun updateGroup(userId: Long, groupId: Long, request: UpdateGroupRequest): GroupResponse {
        dbSessionMapper.setCurrentUserId(userId)
        requireOwner(userId, groupId)
        val updated = groupMapper.update(
            GroupUpdate(groupId, request.name, request.image, request.description, request.joinType?.code)
        )
        if (updated == 0) throw GroupNotFoundException()
        val group = groupMapper.findById(groupId) ?: throw GroupNotFoundException()
        return GroupResponse.from(group, GroupRole.OWNER)
    }

    @Transactional
    fun deleteGroup(userId: Long, groupId: Long) {
        dbSessionMapper.setCurrentUserId(userId)
        requireOwner(userId, groupId)
        requireNotLounge(groupId, "라운지는 삭제할 수 없습니다")
        groupMapper.softDelete(groupId)
    }

    @Transactional
    fun createInvite(userId: Long, groupId: Long, request: CreateInviteRequest): InviteResponse {
        dbSessionMapper.setCurrentUserId(userId)
        requireModerator(userId, groupId)

        val expiresAt = request.expiresInDays?.let { OffsetDateTime.now().plusDays(it.toLong()) }
        val code = generateUniqueInviteCode()
        val record = NewGroupInviteRecord(groupId, code, userId, request.maxUses, expiresAt)
        groupInviteMapper.insert(record)
        return InviteResponse.from(groupInviteMapper.findByCode(code)!!)
    }

    @Transactional
    fun joinByCode(userId: Long, code: String): GroupResponse {
        val invite = groupInviteMapper.findByCode(code) ?: throw InvalidInviteException()
        if (!invite.isUsable(OffsetDateTime.now())) throw InvalidInviteException()

        dbSessionMapper.setCurrentUserId(userId)
        if (userGroupMapper.findRole(userId, invite.groupId) != null) throw AlreadyMemberException()

        userGroupMapper.insert(userId, invite.groupId, GroupRole.MEMBER)
        groupInviteMapper.incrementUsedCount(invite.id)

        val group = groupMapper.findById(invite.groupId) ?: throw GroupNotFoundException()
        return GroupResponse.from(group, GroupRole.MEMBER)
    }

    // 그룹 탐색(레거시 "전체 그룹" 복원): 라운지/삭제 그룹을 뺀 모든 그룹을 노출한다.
    // 가입 여부와 무관하게 보여주고, 카드 버튼 상태를 위해 membership(NONE/PENDING/MEMBER)을 함께 내린다.
    @Transactional
    fun discoverGroups(userId: Long, query: String, sort: String, page: Int, size: Int): List<DiscoverGroupResponse> {
        if (sort !in setOf("recent", "popular")) {
            throw IllegalArgumentException("sort는 recent 또는 popular만 가능합니다")
        }
        dbSessionMapper.setCurrentUserId(userId)
        return groupMapper.findDiscoverGroups(userId, query.trim(), sort, size, page * size)
            .map { DiscoverGroupResponse.from(it) }
    }

    // 레거시 join_type 의미 복원: 자동 승인(0)이면 즉시 가입, 승인제(1)면 신청을 남기고
    // 모더레이터에게 알림을 보낸다. 초대 코드 가입(joinByCode)은 이와 별개로 계속 동작한다.
    @Transactional
    fun joinGroup(userId: Long, groupId: Long): JoinGroupResponse {
        dbSessionMapper.setCurrentUserId(userId)
        val group = groupMapper.findById(groupId) ?: throw GroupNotFoundException()
        if (group.isLounge) throw IllegalArgumentException("라운지는 모든 회원이 자동 가입되는 공간입니다")
        if (userGroupMapper.findRole(userId, groupId) != null) throw AlreadyMemberException()
        if (groupJoinRequestMapper.exists(groupId, userId)) throw AlreadyRequestedException()

        return if (GroupJoinType.fromCode(group.joinType) == GroupJoinType.AUTO_APPROVE) {
            userGroupMapper.insert(userId, groupId, GroupRole.MEMBER)
            JoinGroupResponse(JoinResult.JOINED, GroupResponse.from(group, GroupRole.MEMBER))
        } else {
            groupJoinRequestMapper.insert(groupId, userId)
            val moderatorIds = userGroupMapper.findMembers(groupId)
                .filter { it.role.isModerator }
                .map { it.userId }
            notificationService.notifyAll(moderatorIds, NotificationType.JOIN_REQUEST, NotificationTargetType.GROUP, groupId, actorId = userId)
            JoinGroupResponse(JoinResult.REQUESTED, null)
        }
    }

    @Transactional
    fun cancelJoinRequest(userId: Long, groupId: Long) {
        dbSessionMapper.setCurrentUserId(userId)
        if (groupJoinRequestMapper.delete(groupId, userId) == 0) throw JoinRequestNotFoundException()
    }

    @Transactional
    fun listJoinRequests(userId: Long, groupId: Long): List<JoinRequestResponse> {
        dbSessionMapper.setCurrentUserId(userId)
        requireModerator(userId, groupId)
        return groupJoinRequestMapper.findByGroup(groupId).map { JoinRequestResponse.from(it) }
    }

    @Transactional
    fun approveJoinRequest(userId: Long, groupId: Long, targetUserId: Long) {
        dbSessionMapper.setCurrentUserId(userId)
        requireModerator(userId, groupId)
        if (groupJoinRequestMapper.delete(groupId, targetUserId) == 0) throw JoinRequestNotFoundException()
        userGroupMapper.insert(targetUserId, groupId, GroupRole.MEMBER)
        notificationService.notify(targetUserId, NotificationType.JOIN_APPROVED, NotificationTargetType.GROUP, groupId)
    }

    @Transactional
    fun rejectJoinRequest(userId: Long, groupId: Long, targetUserId: Long) {
        dbSessionMapper.setCurrentUserId(userId)
        requireModerator(userId, groupId)
        if (groupJoinRequestMapper.delete(groupId, targetUserId) == 0) throw JoinRequestNotFoundException()
        notificationService.notify(targetUserId, NotificationType.JOIN_REJECTED, NotificationTargetType.GROUP, groupId)
    }

    @Transactional
    fun listMembers(userId: Long, groupId: Long): List<MemberResponse> {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        return userGroupMapper.findMembers(groupId).map { MemberResponse.from(it) }
    }

    @Transactional
    fun kickMember(userId: Long, groupId: Long, targetUserId: Long) {
        dbSessionMapper.setCurrentUserId(userId)
        val myRole = requireModerator(userId, groupId)
        requireNotLounge(groupId, "라운지에서는 멤버를 강퇴할 수 없습니다")
        if (targetUserId == userId) {
            throw IllegalArgumentException("자기 자신은 강퇴할 수 없습니다. 그룹 탈퇴/삭제를 이용하세요")
        }
        val targetRole = userGroupMapper.findRole(targetUserId, groupId) ?: throw GroupMemberNotFoundException()
        // 계층 준수: 방장은 부방장/멤버를, 부방장은 멤버만 내보낼 수 있다.
        if (targetRole == GroupRole.OWNER || (myRole == GroupRole.ADMIN && targetRole != GroupRole.MEMBER)) {
            throw ForbiddenException()
        }
        userGroupMapper.delete(targetUserId, groupId)
    }

    // 등급 변경은 방장 전용. OWNER 지정(소유권 이전)은 별도 기능으로 미지원.
    @Transactional
    fun updateMemberRole(userId: Long, groupId: Long, targetUserId: Long, newRole: GroupRole) {
        dbSessionMapper.setCurrentUserId(userId)
        requireOwner(userId, groupId)
        requireNotLounge(groupId, "라운지에서는 등급을 변경할 수 없습니다")
        if (newRole == GroupRole.OWNER) throw IllegalArgumentException("방장 권한은 등급 변경으로 넘길 수 없습니다")
        if (targetUserId == userId) throw IllegalArgumentException("자신의 등급은 변경할 수 없습니다")
        val targetRole = userGroupMapper.findRole(targetUserId, groupId) ?: throw GroupMemberNotFoundException()
        if (targetRole == GroupRole.OWNER) throw ForbiddenException()
        userGroupMapper.updateRole(targetUserId, groupId, newRole)
    }

    @Transactional
    fun leaveGroup(userId: Long, groupId: Long) {
        dbSessionMapper.setCurrentUserId(userId)
        val role = requireMembership(userId, groupId)
        requireNotLounge(groupId, "라운지는 탈퇴할 수 없습니다")
        if (role == GroupRole.OWNER) {
            throw IllegalArgumentException("소유자는 그룹을 탈퇴할 수 없습니다. 그룹 삭제를 이용하세요")
        }
        userGroupMapper.delete(userId, groupId)
    }

    private fun requireMembership(userId: Long, groupId: Long): GroupRole =
        userGroupMapper.findRole(userId, groupId) ?: throw GroupNotFoundException()

    private fun requireOwner(userId: Long, groupId: Long) {
        if (requireMembership(userId, groupId) != GroupRole.OWNER) throw ForbiddenException()
    }

    private fun requireModerator(userId: Long, groupId: Long): GroupRole {
        val role = requireMembership(userId, groupId)
        if (!role.isModerator) throw ForbiddenException()
        return role
    }

    private fun requireNotLounge(groupId: Long, message: String) {
        val group = groupMapper.findById(groupId) ?: throw GroupNotFoundException()
        if (group.isLounge) throw IllegalArgumentException(message)
    }

    // 코드 유일성은 INSERT의 UNIQUE 제약을 catch/retry하는 대신 사전 조회로 확인한다.
    // Postgres는 제약 위반이 발생하면 같은 트랜잭션의 이후 명령을 전부 거부하므로
    // 같은 트랜잭션 안에서 실패한 INSERT를 그대로 재시도하면 안 된다.
    private fun generateUniqueInviteCode(): String {
        repeat(5) {
            val code = (1..8).map { inviteCodeChars[secureRandom.nextInt(inviteCodeChars.length)] }.joinToString("")
            if (groupInviteMapper.findByCode(code) == null) return code
        }
        throw IllegalStateException("초대 코드 생성에 반복적으로 실패했습니다")
    }
}
