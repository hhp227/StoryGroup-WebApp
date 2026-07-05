package kr.hhp227.groupsns_webapp.user

import kr.hhp227.groupsns_webapp.common.exception.UserNotFoundException
import kr.hhp227.groupsns_webapp.user.dto.ProfileResponse
import kr.hhp227.groupsns_webapp.user.dto.UpdateProfileRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(private val userMapper: UserMapper) {

    fun getProfile(userId: Long): ProfileResponse =
        ProfileResponse.from(userMapper.findById(userId) ?: throw UserNotFoundException())

    @Transactional
    fun updateProfile(userId: Long, request: UpdateProfileRequest): ProfileResponse {
        val updated = userMapper.updateProfile(
            UserProfileUpdate(userId, request.name, request.profileImg, request.bio, request.statusMessage)
        )
        if (updated == 0) throw UserNotFoundException()
        return getProfile(userId)
    }
}
