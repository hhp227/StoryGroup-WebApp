package kr.hhp227.groupsns_webapp.auth

import kr.hhp227.groupsns_webapp.auth.apple.AppleCallbackState
import kr.hhp227.groupsns_webapp.auth.apple.AppleCodeExchanger
import kr.hhp227.groupsns_webapp.auth.apple.AppleIdentity
import kr.hhp227.groupsns_webapp.auth.apple.AppleLoginCodes
import kr.hhp227.groupsns_webapp.auth.apple.AppleOAuthProperties
import kr.hhp227.groupsns_webapp.auth.apple.AppleTokenVerifier
import kr.hhp227.groupsns_webapp.auth.dto.AppleLoginRequest
import kr.hhp227.groupsns_webapp.auth.google.GoogleAccessTokenVerifier
import kr.hhp227.groupsns_webapp.auth.google.GoogleCodeExchanger
import kr.hhp227.groupsns_webapp.auth.google.GoogleTokenVerifier
import kr.hhp227.groupsns_webapp.common.exception.DuplicateEmailException
import kr.hhp227.groupsns_webapp.common.exception.InvalidAppleTokenException
import kr.hhp227.groupsns_webapp.group.GroupMapper
import kr.hhp227.groupsns_webapp.group.UserGroupMapper
import kr.hhp227.groupsns_webapp.security.JwtTokenProvider
import kr.hhp227.groupsns_webapp.user.NewUserRecord
import kr.hhp227.groupsns_webapp.user.User
import kr.hhp227.groupsns_webapp.user.UserMapper
import kr.hhp227.groupsns_webapp.user.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.OffsetDateTime

// AuthServiceGoogleLoginTest 관례 — 전 의존성 mock, JwtTokenProvider만 실물
class AuthServiceAppleLoginTest {
    private val userMapper = Mockito.mock(UserMapper::class.java)
    private val loginHistoryMapper = Mockito.mock(LoginHistoryMapper::class.java)
    private val oauthAccountMapper = Mockito.mock(OauthAccountMapper::class.java)
    private val appleTokenVerifier = Mockito.mock(AppleTokenVerifier::class.java)
    private val appleCodeExchanger = Mockito.mock(AppleCodeExchanger::class.java)
    private val appleProperties = AppleOAuthProperties(
        "com.hhp227.Application,svc", "svc", "T", "K", "pem", "https://web.example/login", "https://api.example/api/auth/apple/callback"
    )
    private val jwt = JwtTokenProvider("test-secret-test-secret-test-secret-1234", 1_800_000)
    private val service = AuthService(
        userMapper, Mockito.mock(RefreshTokenMapper::class.java), loginHistoryMapper,
        Mockito.mock(GroupMapper::class.java), Mockito.mock(UserGroupMapper::class.java), oauthAccountMapper,
        Mockito.mock(GoogleTokenVerifier::class.java), Mockito.mock(GoogleCodeExchanger::class.java),
        Mockito.mock(GoogleAccessTokenVerifier::class.java),
        appleTokenVerifier, appleCodeExchanger, appleProperties,
        AppleLoginCodes("test-secret-test-secret-test-secret-1234", 60_000),
        Mockito.mock(PasswordEncoder::class.java), jwt, 1_209_600_000
    )
    private val dummyRecord = NewUserRecord("", "", null)

    init {
        Mockito.`when`(oauthAccountMapper.findUserId(Mockito.anyString(), Mockito.anyString())).thenReturn(null)
    }

    private fun identity(email: String? = "a@icloud.com", verified: Boolean = true, relay: Boolean = false, aud: String = "com.hhp227.Application") =
        AppleIdentity(sub = "ap-1", email = email, emailVerified = verified, isPrivateEmail = relay, audience = aud)

    private fun user(id: Long = 7, email: String = "a@icloud.com") = User(
        id = id, name = "테스터", email = email, passwordHash = null, status = 0, role = UserRole.USER,
        profileImg = null, bio = null, statusMessage = null, createdAt = OffsetDateTime.now(), deletedAt = null
    )

    private fun request(code: String? = "auth-code", type: String = "IOS", first: String? = null, last: String? = null) =
        AppleLoginRequest(identityToken = "tok", authorizationCode = code, clientType = type, firstName = first, lastName = last)

    private fun stubInsertAssigningId(id: Long) {
        Mockito.`when`(userMapper.insert(any(NewUserRecord::class.java) ?: dummyRecord)).thenAnswer {
            (it.arguments[0] as NewUserRecord).id = id
            1
        }
        Mockito.`when`(userMapper.findById(id)).thenReturn(user(id = id))
    }

    private fun capturedInsert(): NewUserRecord {
        val captor = ArgumentCaptor.forClass(NewUserRecord::class.java)
        Mockito.verify(userMapper).insert(captor.capture() ?: dummyRecord)
        return captor.value
    }

    @Test
    fun `기존 연결이면 그 사용자로 로그인하고 교환한 refresh token을 저장`() {
        Mockito.`when`(appleTokenVerifier.verify("tok")).thenReturn(identity(email = null))
        Mockito.`when`(oauthAccountMapper.findUserId("APPLE", "ap-1")).thenReturn(7L)
        Mockito.`when`(userMapper.findById(7)).thenReturn(user())
        Mockito.`when`(appleCodeExchanger.exchange("auth-code", "com.hhp227.Application", null)).thenReturn("apple-rt")

        val tokens = service.loginWithApple(request(), "1.1.1.1", "ua")

        assertEquals(7L, jwt.getUserId(tokens.accessToken))
        Mockito.verify(oauthAccountMapper).updateToken("APPLE", "ap-1", "apple-rt", "com.hhp227.Application")
        Mockito.verify(loginHistoryMapper).insert(7, "1.1.1.1", "ua", true)
    }

    @Test
    fun `웹은 web-redirect-uri로 교환한다`() {
        Mockito.`when`(appleTokenVerifier.verify("tok")).thenReturn(identity(aud = "svc"))
        Mockito.`when`(oauthAccountMapper.findUserId("APPLE", "ap-1")).thenReturn(7L)
        Mockito.`when`(userMapper.findById(7)).thenReturn(user())

        service.loginWithApple(request(type = "WEB"), null, null)

        Mockito.verify(appleCodeExchanger).exchange("auth-code", "svc", "https://web.example/login")
    }

    @Test
    fun `교환 실패(null)여도 로그인은 되고 토큰은 저장하지 않는다`() {
        Mockito.`when`(appleTokenVerifier.verify("tok")).thenReturn(identity())
        Mockito.`when`(oauthAccountMapper.findUserId("APPLE", "ap-1")).thenReturn(7L)
        Mockito.`when`(userMapper.findById(7)).thenReturn(user())

        service.loginWithApple(request(), null, null)

        Mockito.verify(oauthAccountMapper, Mockito.never())
            .updateToken(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString())
    }

    @Test
    fun `같은 검증 이메일 사용자에 자동 연결`() {
        Mockito.`when`(appleTokenVerifier.verify("tok")).thenReturn(identity())
        Mockito.`when`(userMapper.findByEmail("a@icloud.com")).thenReturn(user())

        service.loginWithApple(request(), null, null)

        Mockito.verify(oauthAccountMapper).insert(7, "APPLE", "ap-1")
        Mockito.verify(userMapper, Mockito.never()).insert(any(NewUserRecord::class.java) ?: dummyRecord)
    }

    @Test
    fun `신규 — 전달된 한글 이름으로 생성, 프로필 이미지 없음`() {
        Mockito.`when`(appleTokenVerifier.verify("tok")).thenReturn(identity())
        stubInsertAssigningId(9)

        service.loginWithApple(request(first = "길동", last = "홍"), null, null)

        val record = capturedInsert()
        assertEquals("홍길동", record.name)
        assertEquals("a@icloud.com", record.email)
        assertNull(record.passwordHash)
        assertNull(record.profileImg)
        Mockito.verify(oauthAccountMapper).insert(9, "APPLE", "ap-1")
    }

    @Test
    fun `신규 — 이름 없음 + relay면 애플 사용자, 일반 이메일이면 앞부분`() {
        Mockito.`when`(appleTokenVerifier.verify("tok")).thenReturn(identity(email = "x@privaterelay.appleid.com", relay = true))
        stubInsertAssigningId(9)
        service.loginWithApple(request(), null, null)
        assertEquals("애플 사용자", capturedInsert().name)
    }

    @Test
    fun `신규 — 이름 없음 + 일반 이메일이면 이메일 앞부분`() {
        Mockito.`when`(appleTokenVerifier.verify("tok")).thenReturn(identity(email = "me@icloud.com"))
        stubInsertAssigningId(9)
        service.loginWithApple(request(), null, null)
        assertEquals("me", capturedInsert().name)
    }

    @Test
    fun `미검증 이메일 충돌은 409`() {
        Mockito.`when`(appleTokenVerifier.verify("tok")).thenReturn(identity(verified = false))
        Mockito.`when`(userMapper.findByEmail("a@icloud.com")).thenReturn(user())
        assertThrows(DuplicateEmailException::class.java) { service.loginWithApple(request(), null, null) }
    }

    @Test
    fun `연결도 이메일도 없으면 401`() {
        Mockito.`when`(appleTokenVerifier.verify("tok")).thenReturn(identity(email = null))
        assertThrows(InvalidAppleTokenException::class.java) { service.loginWithApple(request(), null, null) }
    }

    @Test
    fun `연결된 사용자가 탈퇴했으면 401`() {
        Mockito.`when`(appleTokenVerifier.verify("tok")).thenReturn(identity())
        Mockito.`when`(oauthAccountMapper.findUserId("APPLE", "ap-1")).thenReturn(7L)
        Mockito.`when`(userMapper.findById(7)).thenReturn(null)
        assertThrows(InvalidAppleTokenException::class.java) { service.loginWithApple(request(), null, null) }
    }

    private val androidState = AppleCallbackState(AppleCallbackState.Platform.ANDROID, null, "nn", AppleLoginCodes.hashVerifier("ver"))

    @Test
    fun `콜백 성공 — 코드를 담아 복귀하고, 그 코드+verifier로 토큰 교환`() {
        Mockito.`when`(appleTokenVerifier.verify("idt")).thenReturn(identity(aud = "svc"))
        Mockito.`when`(oauthAccountMapper.findUserId("APPLE", "ap-1")).thenReturn(7L)
        Mockito.`when`(userMapper.findById(7)).thenReturn(user())

        val url = service.completeAppleCallback(androidState, "idt", "c1", null, null)

        assertTrue(url.startsWith("storygroup://auth/apple?code="))
        assertTrue(url.endsWith("&nonce=nn"))
        Mockito.verify(appleCodeExchanger).exchange("c1", "svc", "https://api.example/api/auth/apple/callback")
        val code = url.substringAfter("code=").substringBefore("&")
        val tokens = service.exchangeAppleCode(code, "ver", "2.2.2.2", "ua")
        assertEquals(7L, jwt.getUserId(tokens.accessToken))
        Mockito.verify(loginHistoryMapper).insert(7, "2.2.2.2", "ua", true)
    }

    @Test
    fun `콜백 — 신규 가입은 user JSON 이름을 쓴다`() {
        Mockito.`when`(appleTokenVerifier.verify("idt")).thenReturn(identity(aud = "svc"))
        stubInsertAssigningId(9)
        service.completeAppleCallback(androidState, "idt", "c1", """{"name":{"firstName":"John","lastName":"Smith"}}""", null)
        assertEquals("John Smith", capturedInsert().name)
    }

    @Test
    fun `콜백 — 애플 오류·검증 실패·미설정·이메일 충돌은 error로 복귀`() {
        assertEquals(
            "storygroup://auth/apple?error=user_cancelled_authorize&nonce=nn",
            service.completeAppleCallback(androidState, null, null, null, "user_cancelled_authorize")
        )
        Mockito.`when`(appleTokenVerifier.verify("bad")).thenThrow(InvalidAppleTokenException())
        assertTrue(service.completeAppleCallback(androidState, "bad", null, null, null).contains("error=invalid_token"))
        Mockito.`when`(appleTokenVerifier.verify("nc")).thenThrow(InvalidAppleTokenException(InvalidAppleTokenException.NOT_CONFIGURED))
        assertTrue(service.completeAppleCallback(androidState, "nc", null, null, null).contains("error=not_configured"))
        Mockito.`when`(appleTokenVerifier.verify("dup")).thenReturn(identity(verified = false))
        Mockito.`when`(userMapper.findByEmail("a@icloud.com")).thenReturn(user())
        assertTrue(service.completeAppleCallback(androidState, "dup", null, null, null).contains("error=duplicate_email"))
        assertTrue(service.completeAppleCallback(androidState, null, null, null, null).contains("error=invalid_token"))
    }

    @Test
    fun `교환 — 탈퇴한 사용자면 401`() {
        val code = AppleLoginCodes("test-secret-test-secret-test-secret-1234", 60_000).issue(7, AppleLoginCodes.hashVerifier("ver"))
        Mockito.`when`(userMapper.findById(7)).thenReturn(null)
        assertThrows(InvalidAppleTokenException::class.java) { service.exchangeAppleCode(code, "ver", null, null) }
    }
}
