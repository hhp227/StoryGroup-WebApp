package kr.hhp227.groupsns_webapp.block

import kr.hhp227.groupsns_webapp.block.dto.BlockedUserResponse
import kr.hhp227.groupsns_webapp.common.db.DbSessionMapper
import kr.hhp227.groupsns_webapp.common.exception.AlreadyBlockedException
import kr.hhp227.groupsns_webapp.common.exception.BlockNotFoundException
import kr.hhp227.groupsns_webapp.common.exception.UserNotFoundException
import kr.hhp227.groupsns_webapp.user.UserMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BlockService(
    private val userBlockMapper: UserBlockMapper,
    private val userMapper: UserMapper,
    private val dbSessionMapper: DbSessionMapper
) {
    @Transactional
    fun listBlockedUsers(userId: Long): List<BlockedUserResponse> {
        dbSessionMapper.setCurrentUserId(userId)
        return userBlockMapper.findBlockedUsers(userId).map { BlockedUserResponse.from(it) }
    }

    @Transactional
    fun blockUser(userId: Long, targetUserId: Long) {
        if (userId == targetUserId) throw IllegalArgumentException("자기 자신은 차단할 수 없습니다")
        dbSessionMapper.setCurrentUserId(userId)
        userMapper.findById(targetUserId) ?: throw UserNotFoundException()
        if (userBlockMapper.exists(userId, targetUserId)) throw AlreadyBlockedException()
        userBlockMapper.insert(userId, targetUserId)
    }

    @Transactional
    fun unblockUser(userId: Long, targetUserId: Long) {
        dbSessionMapper.setCurrentUserId(userId)
        if (userBlockMapper.delete(userId, targetUserId) == 0) throw BlockNotFoundException()
    }
}
