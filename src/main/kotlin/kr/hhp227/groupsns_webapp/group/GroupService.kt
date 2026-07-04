package kr.hhp227.groupsns_webapp.group

import kr.hhp227.groupsns_webapp.chat.ChatRoomMapper
import kr.hhp227.groupsns_webapp.chat.NewChatRoomRecord
import kr.hhp227.groupsns_webapp.common.db.DbSessionMapper
import kr.hhp227.groupsns_webapp.common.exception.AlreadyMemberException
import kr.hhp227.groupsns_webapp.common.exception.ForbiddenException
import kr.hhp227.groupsns_webapp.common.exception.GroupMemberNotFoundException
import kr.hhp227.groupsns_webapp.common.exception.GroupNotFoundException
import kr.hhp227.groupsns_webapp.common.exception.InvalidInviteException
import kr.hhp227.groupsns_webapp.group.dto.CreateGroupRequest
import kr.hhp227.groupsns_webapp.group.dto.CreateInviteRequest
import kr.hhp227.groupsns_webapp.group.dto.GroupResponse
import kr.hhp227.groupsns_webapp.group.dto.InviteResponse
import kr.hhp227.groupsns_webapp.group.dto.MemberResponse
import kr.hhp227.groupsns_webapp.group.dto.UpdateGroupRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.OffsetDateTime

@Service
class GroupService(
    private val groupMapper: GroupMapper,
    private val userGroupMapper: UserGroupMapper,
    private val groupInviteMapper: GroupInviteMapper,
    private val chatRoomMapper: ChatRoomMapper,
    private val dbSessionMapper: DbSessionMapper
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
    fun listMyGroups(userId: Long): List<GroupResponse> {
        dbSessionMapper.setCurrentUserId(userId)
        return userGroupMapper.findGroupsForUser(userId).map { GroupResponse.from(it) }
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
        val updated = groupMapper.update(GroupUpdate(groupId, request.name, request.image, request.description))
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
        requireOwner(userId, groupId)

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

    @Transactional
    fun listMembers(userId: Long, groupId: Long): List<MemberResponse> {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        return userGroupMapper.findMembers(groupId).map { MemberResponse.from(it) }
    }

    @Transactional
    fun kickMember(userId: Long, groupId: Long, targetUserId: Long) {
        dbSessionMapper.setCurrentUserId(userId)
        requireOwner(userId, groupId)
        requireNotLounge(groupId, "라운지에서는 멤버를 강퇴할 수 없습니다")
        if (targetUserId == userId) {
            throw IllegalArgumentException("자기 자신은 강퇴할 수 없습니다. 그룹 삭제를 이용하세요")
        }
        val removed = userGroupMapper.delete(targetUserId, groupId)
        if (removed == 0) throw GroupMemberNotFoundException()
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
