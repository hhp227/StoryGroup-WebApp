package kr.hhp227.groupsns_webapp.auth

import kr.hhp227.groupsns_webapp.auth.apple.AppleCodeExchanger
import kr.hhp227.groupsns_webapp.auth.apple.AppleLoginCodes
import kr.hhp227.groupsns_webapp.auth.apple.AppleNames
import kr.hhp227.groupsns_webapp.auth.apple.AppleOAuthProperties
import kr.hhp227.groupsns_webapp.auth.apple.AppleTokenVerifier
import kr.hhp227.groupsns_webapp.auth.dto.AppleLoginRequest
import kr.hhp227.groupsns_webapp.auth.dto.LoginRequest
import kr.hhp227.groupsns_webapp.auth.google.GoogleAccessTokenVerifier
import kr.hhp227.groupsns_webapp.auth.google.GoogleCodeExchanger
import kr.hhp227.groupsns_webapp.auth.google.GoogleIdentity
import kr.hhp227.groupsns_webapp.auth.google.GoogleTokenVerifier
import kr.hhp227.groupsns_webapp.auth.dto.RefreshTokenRequest
import kr.hhp227.groupsns_webapp.auth.dto.RegisterRequest
import kr.hhp227.groupsns_webapp.auth.dto.TokenResponse
import kr.hhp227.groupsns_webapp.auth.dto.UserSummaryResponse
import kr.hhp227.groupsns_webapp.common.exception.DuplicateEmailException
import kr.hhp227.groupsns_webapp.common.exception.InvalidAppleTokenException
import kr.hhp227.groupsns_webapp.common.exception.InvalidCredentialsException
import kr.hhp227.groupsns_webapp.common.exception.InvalidGoogleTokenException
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
    private val oauthAccountMapper: OauthAccountMapper,
    private val googleTokenVerifier: GoogleTokenVerifier,
    private val googleCodeExchanger: GoogleCodeExchanger,
    private val googleAccessTokenVerifier: GoogleAccessTokenVerifier,
    private val appleTokenVerifier: AppleTokenVerifier,
    private val appleCodeExchanger: AppleCodeExchanger,
    private val appleProperties: AppleOAuthProperties,
    private val appleLoginCodes: AppleLoginCodes,
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
        val user = createUser(
            NewUserRecord(name = request.name, email = request.email, passwordHash = passwordEncoder.encode(request.password))
        )
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
    fun loginWithGoogleIdToken(idToken: String, ipAddress: String?, userAgent: String?): TokenResponse =
        loginWithGoogle(googleTokenVerifier.verify(idToken), ipAddress, userAgent)

    // 웹 커스텀 버튼(GIS 토큰 클라이언트 팝업) — ID 토큰 대신 액세스 토큰을 받는다
    @Transactional
    fun loginWithGoogleAccessToken(accessToken: String, ipAddress: String?, userAgent: String?): TokenResponse =
        loginWithGoogle(googleAccessTokenVerifier.verify(accessToken), ipAddress, userAgent)

    // Desktop 루프백 PKCE — 교환된 id_token도 같은 검증(aud 포함)을 거친다
    @Transactional
    fun loginWithGoogleCode(
        code: String,
        codeVerifier: String,
        redirectUri: String,
        ipAddress: String?,
        userAgent: String?
    ): TokenResponse =
        loginWithGoogleIdToken(googleCodeExchanger.exchange(code, codeVerifier, redirectUri), ipAddress, userAgent)

    // iOS 네이티브·웹 팝업 — id_token 검증 → 계정 결정 → 코드 교환(폐기 대비) → 토큰 발급
    @Transactional
    fun loginWithApple(request: AppleLoginRequest, ipAddress: String?, userAgent: String?): TokenResponse {
        // 인가 때 쓴 redirect_uri로 교환해야 한다 — iOS 네이티브는 redirect_uri가 없다
        val redirectUri = if (request.clientType == "WEB") appleProperties.webRedirectUri.ifEmpty { null } else null
        val user = resolveAppleUser(request.identityToken, request.authorizationCode, redirectUri, request.firstName, request.lastName)
        loginHistoryMapper.insert(user.id, ipAddress, userAgent, true)
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

    // 구글 3경로 공통 — 계정 결정 후 이력·토큰 발급
    private fun loginWithGoogle(identity: GoogleIdentity, ipAddress: String?, userAgent: String?): TokenResponse {
        val user = resolveOauthUser(
            OauthProvider.GOOGLE,
            OauthIdentity(identity.sub, identity.email, identity.emailVerified, identity.name, identity.picture)
        ) { InvalidGoogleTokenException() }
        loginHistoryMapper.insert(user.id, ipAddress, userAgent, true)
        return issueTokens(user, userAgent)
    }

    // 계정 결정(구글 설계 §2.3, 애플 설계 §2.3): 기존 연결 → 검증된 이메일 자동 연결 → 신규 가입
    private fun resolveOauthUser(provider: String, identity: OauthIdentity, invalid: () -> RuntimeException): User {
        val linkedUserId = oauthAccountMapper.findUserId(provider, identity.sub)
        if (linkedUserId != null) {
            // 탈퇴 사용자는 findById가 걸러낸다(연결 행은 탈퇴 시 지워지지만 방어)
            return userMapper.findById(linkedUserId) ?: throw invalid()
        }
        // 연결이 없으면 이메일이 있어야 연결·가입할 수 있다(users.email NOT NULL)
        val email = identity.email ?: throw invalid()
        val existing = userMapper.findByEmail(email)
        return when {
            existing != null && identity.emailVerified -> existing
            // provider가 이메일 소유를 보증하지 않으면 남의 계정을 가져가는 경로가 된다
            existing != null -> throw DuplicateEmailException()
            else -> createUser(
                NewUserRecord(
                    name = (identity.name?.takeIf { it.isNotBlank() } ?: email.substringBefore('@')).take(50),
                    email = email,
                    passwordHash = null,
                    profileImg = identity.picture
                )
            )
        }.also { oauthAccountMapper.insert(it.id, provider, identity.sub) }
    }

    // 애플 공통(iOS·웹·콜백): 계정 결정 + refresh token 저장. 교환 실패는 로그인을 막지 않는다(설계 §2.2)
    private fun resolveAppleUser(
        identityToken: String,
        authorizationCode: String?,
        redirectUri: String?,
        firstName: String?,
        lastName: String?
    ): User {
        val identity = appleTokenVerifier.verify(identityToken)
        val user = resolveOauthUser(
            OauthProvider.APPLE,
            OauthIdentity(
                sub = identity.sub,
                email = identity.email,
                emailVerified = identity.emailVerified,
                name = AppleNames.displayName(firstName, lastName, identity.email, identity.isPrivateEmail),
                picture = null
            )
        ) { InvalidAppleTokenException() }
        authorizationCode?.takeIf { it.isNotBlank() }
            ?.let { appleCodeExchanger.exchange(it, identity.audience, redirectUri) }
            ?.let { oauthAccountMapper.updateToken(OauthProvider.APPLE, identity.sub, it, identity.audience) }
        return user
    }

    private fun createUser(record: NewUserRecord): User {
        userMapper.insert(record)
        val user = userMapper.findById(record.id) ?: throw IllegalStateException("방금 생성한 유저를 찾을 수 없습니다")

        // 라운지(전체 공개 피드)는 그룹 하나일 뿐이라 신규 가입자를 자동으로 멤버 가입시킨다.
        // 마이그레이션 전이라 라운지가 아직 없는 경우(findLounge() == null)는 건너뛴다.
        groupMapper.findLounge()?.let { lounge ->
            userGroupMapper.insert(user.id, lounge.id, GroupRole.MEMBER)
        }
        return user
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
