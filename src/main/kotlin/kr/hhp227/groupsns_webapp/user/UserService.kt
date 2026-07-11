package kr.hhp227.groupsns_webapp.user

import kr.hhp227.groupsns_webapp.auth.RefreshTokenMapper
import kr.hhp227.groupsns_webapp.common.exception.UserNotFoundException
import kr.hhp227.groupsns_webapp.user.dto.ChangePasswordRequest
import kr.hhp227.groupsns_webapp.user.dto.ProfileResponse
import kr.hhp227.groupsns_webapp.user.dto.PublicProfileResponse
import kr.hhp227.groupsns_webapp.user.dto.UpdateProfileRequest
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userMapper: UserMapper,
    private val refreshTokenMapper: RefreshTokenMapper,
    private val passwordEncoder: PasswordEncoder
) {

    fun getProfile(userId: Long): ProfileResponse =
        ProfileResponse.from(userMapper.findById(userId) ?: throw UserNotFoundException())

    // 다른 사용자가 보는 공개 프로필 — 이메일 등 민감 정보는 제외(PublicProfileResponse가 담당).
    fun getPublicProfile(targetUserId: Long): PublicProfileResponse {
        val user = userMapper.findById(targetUserId)?.takeIf { it.deletedAt == null }
            ?: throw UserNotFoundException()
        return PublicProfileResponse.from(user)
    }

    @Transactional
    fun updateProfile(userId: Long, request: UpdateProfileRequest): ProfileResponse {
        val updated = userMapper.updateProfile(
            UserProfileUpdate(userId, request.name, request.profileImg, request.bio, request.statusMessage)
        )
        if (updated == 0) throw UserNotFoundException()
        return getProfile(userId)
    }

    @Transactional
    fun changePassword(userId: Long, request: ChangePasswordRequest) {
        val user = userMapper.findById(userId) ?: throw UserNotFoundException()
        val currentHash = user.passwordHash
            ?: throw IllegalArgumentException("소셜 로그인 계정은 비밀번호를 변경할 수 없습니다")
        if (!passwordEncoder.matches(request.currentPassword, currentHash)) {
            throw IllegalArgumentException("현재 비밀번호가 올바르지 않습니다")
        }
        userMapper.updatePassword(userId, passwordEncoder.encode(request.newPassword))
        // 유출된 비밀번호로 유지되던 다른 기기 세션까지 함께 끊는다. 현재 액세스 토큰은
        // 만료(30분)까지 유효하지만 리프레시가 안 되므로 그 뒤로는 재로그인이 필요하다.
        refreshTokenMapper.revokeAllForUser(userId)
    }
}
