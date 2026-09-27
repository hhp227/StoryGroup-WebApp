package kr.hhp227.groupsns_webapp.user

import kr.hhp227.groupsns_webapp.auth.OauthAccountMapper
import kr.hhp227.groupsns_webapp.auth.OauthProvider
import kr.hhp227.groupsns_webapp.auth.RefreshTokenMapper
import kr.hhp227.groupsns_webapp.auth.apple.AppleTokenRevoker
import kr.hhp227.groupsns_webapp.common.exception.OwnedGroupsExistException
import kr.hhp227.groupsns_webapp.common.exception.UserNotFoundException
import kr.hhp227.groupsns_webapp.friend.UserFriendMapper
import kr.hhp227.groupsns_webapp.group.UserGroupMapper
import kr.hhp227.groupsns_webapp.push.PushTokenMapper
import kr.hhp227.groupsns_webapp.user.dto.ChangePasswordRequest
import kr.hhp227.groupsns_webapp.user.dto.DeleteAccountRequest
import kr.hhp227.groupsns_webapp.user.dto.ProfileResponse
import kr.hhp227.groupsns_webapp.user.dto.PublicProfileResponse
import kr.hhp227.groupsns_webapp.user.dto.PushPreferencesResponse
import kr.hhp227.groupsns_webapp.user.dto.UpdateProfileRequest
import kr.hhp227.groupsns_webapp.user.dto.UpdatePushPreferencesRequest
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Service
class UserService(
    private val userMapper: UserMapper,
    private val userGroupMapper: UserGroupMapper,
    private val refreshTokenMapper: RefreshTokenMapper,
    private val pushTokenMapper: PushTokenMapper,
    private val userFriendMapper: UserFriendMapper,
    private val passwordEncoder: PasswordEncoder,
    private val oauthAccountMapper: OauthAccountMapper,
    private val appleTokenRevoker: AppleTokenRevoker
) {
    private val log = LoggerFactory.getLogger(UserService::class.java)

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

    // 즉시 탈퇴(설계 §2) — 단일 트랜잭션: 검증 → 익명화 → 연관 정리. 콘텐츠(게시글·댓글·채팅)는
    // 행을 남겨 "탈퇴한 사용자"로 표시된다. JWT 필터가 findById(deleted 필터)를 타므로 즉시 전 요청 차단.
    @Transactional
    fun deleteAccount(userId: Long, request: DeleteAccountRequest) {
        val user = userMapper.findById(userId) ?: throw UserNotFoundException()
        val hash = user.passwordHash
        if (hash != null) {
            val password = request.password
            if (password.isNullOrEmpty() || !passwordEncoder.matches(password, hash)) {
                throw IllegalArgumentException("현재 비밀번호가 올바르지 않습니다")
            }
        } else if (request.confirmText?.trim() != DELETE_CONFIRM_TEXT) {
            // 구글 전용 계정 — 비밀번호가 없어 확인 문구로 대체(액세스 토큰 인증은 이미 통과)
            throw IllegalArgumentException("확인 문구가 일치하지 않습니다")
        }
        val ownedGroupNames = userGroupMapper.findOwnedGroupNames(userId)
        if (ownedGroupNames.isNotEmpty()) throw OwnedGroupsExistException(ownedGroupNames)

        // 연결 행은 아래 deleteOauthAccounts가 지우므로 먼저 읽어 둔다. 폐기는 커밋 후(롤백되면 폐기하지 않음) — 애플 설계 §2.5
        val appleTokens = oauthAccountMapper.findTokens(userId, OauthProvider.APPLE)
        userMapper.anonymize(userId)
        refreshTokenMapper.revokeAllForUser(userId)
        pushTokenMapper.deleteAllForUser(userId)
        userGroupMapper.deleteAllForUser(userId)
        userFriendMapper.deleteAllInvolving(userId)
        userMapper.deleteOauthAccounts(userId)
        if (appleTokens.isNotEmpty()) {
            afterCommit {
                appleTokens.forEach { token ->
                    // 5.1.1(v) — 실패해도 탈퇴는 이미 끝났다. 로그로만 추적
                    runCatching { appleTokenRevoker.revoke(token.refreshToken, token.clientId) }
                        .onFailure { log.warn("애플 토큰 폐기 실패 userId={}: {}", userId, it.message) }
                }
            }
        }
    }

    // 푸시 종류별 on/off(설계 §2) — 계정 단위. 발송 게이트(PushBroadcaster)가 같은 매퍼로 읽는다
    fun getPushPreferences(userId: Long): PushPreferencesResponse =
        PushPreferencesResponse.from(userMapper.findPushPreferences(userId) ?: throw UserNotFoundException())

    @Transactional
    fun updatePushPreferences(userId: Long, request: UpdatePushPreferencesRequest) {
        // @Valid가 컨트롤러에서 null을 이미 걸렀다 — 직접 호출 경로 대비 방어(IllegalArgumentException → 400)
        val chatEnabled = requireNotNull(request.chatEnabled) { "chatEnabled는 필수입니다" }
        val activityEnabled = requireNotNull(request.activityEnabled) { "activityEnabled는 필수입니다" }
        val updated = userMapper.updatePushPreferences(userId, chatEnabled, activityEnabled)
        if (updated == 0) throw UserNotFoundException()
    }

    // 트랜잭션 안이면 커밋 후, 밖(단위 테스트)이면 즉시
    private fun afterCommit(block: () -> Unit) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() = block()
            })
        } else {
            block()
        }
    }

    companion object {
        // 비밀번호 없는 계정의 탈퇴 확인 문구 — 클라 3종(웹·KMP·iOS)이 같은 값을 입력받는다
        const val DELETE_CONFIRM_TEXT = "탈퇴"
    }
}
