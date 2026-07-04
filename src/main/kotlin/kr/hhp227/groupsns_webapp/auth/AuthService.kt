package kr.hhp227.groupsns_webapp.auth

import kr.hhp227.groupsns_webapp.auth.dto.LoginRequest
import kr.hhp227.groupsns_webapp.auth.dto.RefreshTokenRequest
import kr.hhp227.groupsns_webapp.auth.dto.RegisterRequest
import kr.hhp227.groupsns_webapp.auth.dto.TokenResponse
import kr.hhp227.groupsns_webapp.auth.dto.UserSummaryResponse
import kr.hhp227.groupsns_webapp.common.exception.DuplicateEmailException
import kr.hhp227.groupsns_webapp.common.exception.InvalidCredentialsException
import kr.hhp227.groupsns_webapp.common.exception.InvalidRefreshTokenException
import kr.hhp227.groupsns_webapp.group.GroupMapper
import kr.hhp227.groupsns_webapp.group.GroupRole
import kr.hhp227.groupsns_webapp.group.UserGroupMapper
import kr.hhp227.groupsns_webapp.security.JwtTokenProvider
import kr.hhp227.groupsns_webapp.user.NewUserRecord
import kr.hhp227.groupsns_webapp.user.User
import kr.hhp227.groupsns_webapp.user.UserMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Duration
import java.time.OffsetDateTime
import java.util.Base64

@Service
class AuthService(
    private val userMapper: UserMapper,
    private val refreshTokenMapper: RefreshTokenMapper,
    private val loginHistoryMapper: LoginHistoryMapper,
    private val groupMapper: GroupMapper,
    private val userGroupMapper: UserGroupMapper,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    @Value("\${jwt.refresh-token-expiration-ms}") private val refreshTokenExpirationMs: Long
) {
    private val secureRandom = SecureRandom()

    @Transactional
    fun register(request: RegisterRequest): UserSummaryResponse {
        if (userMapper.existsByEmail(request.email)) {
            throw DuplicateEmailException()
        }
        val record = NewUserRecord(
            name = request.name,
            email = request.email,
            passwordHash = passwordEncoder.encode(request.password)
        )
        userMapper.insert(record)
        val user = userMapper.findById(record.id) ?: throw IllegalStateException("방금 생성한 유저를 찾을 수 없습니다")

        // 라운지(전체 공개 피드)는 그룹 하나일 뿐이라 신규 가입자를 자동으로 멤버 가입시킨다.
        // 마이그레이션 전이라 라운지가 아직 없는 경우(findLounge() == null)는 건너뛴다.
        groupMapper.findLounge()?.let { lounge ->
            userGroupMapper.insert(user.id, lounge.id, GroupRole.MEMBER)
        }

        return UserSummaryResponse.from(user)
    }

    @Transactional
    fun login(request: LoginRequest, ipAddress: String?, userAgent: String?): TokenResponse {
        val user = userMapper.findByEmail(request.email)
        val passwordHash = user?.passwordHash
        val passwordMatches = passwordHash != null && passwordEncoder.matches(request.password, passwordHash)

        if (user != null) {
            loginHistoryMapper.insert(user.id, ipAddress, userAgent, passwordMatches)
        }
        if (user == null || !passwordMatches) {
            throw InvalidCredentialsException()
        }
        return issueTokens(user, userAgent)
    }

    @Transactional
    fun refresh(request: RefreshTokenRequest, userAgent: String?): TokenResponse {
        val tokenHash = hashToken(request.refreshToken)
        val stored = refreshTokenMapper.findByTokenHash(tokenHash) ?: throw InvalidRefreshTokenException()
        if (!stored.isUsable(OffsetDateTime.now())) {
            throw InvalidRefreshTokenException()
        }
        val user = userMapper.findById(stored.userId) ?: throw InvalidRefreshTokenException()

        // 회전(rotation): 재사용 방지를 위해 기존 리프레시 토큰은 즉시 폐기하고 새로 발급
        refreshTokenMapper.revoke(stored.id)
        return issueTokens(user, userAgent)
    }

    @Transactional
    fun logout(request: RefreshTokenRequest) {
        val tokenHash = hashToken(request.refreshToken)
        val stored = refreshTokenMapper.findByTokenHash(tokenHash) ?: return
        refreshTokenMapper.revoke(stored.id)
    }

    private fun issueTokens(user: User, deviceInfo: String?): TokenResponse {
        val accessToken = jwtTokenProvider.generateAccessToken(user.id, user.email)
        val rawRefreshToken = generateOpaqueToken()
        val record = NewRefreshTokenRecord(
            userId = user.id,
            tokenHash = hashToken(rawRefreshToken),
            deviceInfo = deviceInfo?.take(255),
            expiresAt = OffsetDateTime.now().plus(Duration.ofMillis(refreshTokenExpirationMs))
        )
        refreshTokenMapper.insert(record)
        return TokenResponse(
            accessToken = accessToken,
            refreshToken = rawRefreshToken,
            expiresIn = jwtTokenProvider.accessTokenExpirationSeconds()
        )
    }

    private fun generateOpaqueToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
