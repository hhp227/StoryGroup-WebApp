package kr.hhp227.groupsns_webapp.friend

import kr.hhp227.groupsns_webapp.common.db.DbSessionMapper
import kr.hhp227.groupsns_webapp.common.exception.AlreadyFriendException
import kr.hhp227.groupsns_webapp.common.exception.FriendNotFoundException
import kr.hhp227.groupsns_webapp.common.exception.UserNotFoundException
import kr.hhp227.groupsns_webapp.friend.dto.FriendResponse
import kr.hhp227.groupsns_webapp.realtime.UserPresenceTracker
import kr.hhp227.groupsns_webapp.user.UserMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

// 친구는 단방향 등록(즐겨찾기 성격, 승인 절차 없음) — 차단(BlockService)과 같은 구조.
@Service
class FriendService(
    private val userFriendMapper: UserFriendMapper,
    private val userMapper: UserMapper,
    private val dbSessionMapper: DbSessionMapper,
    private val userPresenceTracker: UserPresenceTracker
) {
    @Transactional
    fun listFriends(userId: Long): List<FriendResponse> {
        dbSessionMapper.setCurrentUserId(userId)
        return userFriendMapper.findFriends(userId)
            .map { FriendResponse.from(it, online = userPresenceTracker.isOnline(it.userId)) }
    }

    @Transactional
    fun addFriend(userId: Long, targetUserId: Long) {
        if (userId == targetUserId) throw IllegalArgumentException("자기 자신은 친구로 등록할 수 없습니다")
        dbSessionMapper.setCurrentUserId(userId)
        userMapper.findById(targetUserId) ?: throw UserNotFoundException()
        if (userFriendMapper.exists(userId, targetUserId)) throw AlreadyFriendException()
        userFriendMapper.insert(userId, targetUserId)
    }

    @Transactional
    fun removeFriend(userId: Long, targetUserId: Long) {
        dbSessionMapper.setCurrentUserId(userId)
        if (userFriendMapper.delete(userId, targetUserId) == 0) throw FriendNotFoundException()
    }
}
