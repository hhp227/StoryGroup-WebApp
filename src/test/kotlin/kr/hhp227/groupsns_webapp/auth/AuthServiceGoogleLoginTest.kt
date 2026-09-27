package kr.hhp227.groupsns_webapp.auth

import kr.hhp227.groupsns_webapp.auth.apple.AppleCodeExchanger
import kr.hhp227.groupsns_webapp.auth.apple.AppleLoginCodes
import kr.hhp227.groupsns_webapp.auth.apple.AppleOAuthProperties
import kr.hhp227.groupsns_webapp.auth.apple.AppleTokenVerifier
import kr.hhp227.groupsns_webapp.auth.google.GoogleAccessTokenVerifier
import kr.hhp227.groupsns_webapp.auth.google.GoogleCodeExchanger
import kr.hhp227.groupsns_webapp.auth.google.GoogleIdentity
import kr.hhp227.groupsns_webapp.auth.google.GoogleTokenVerifier
import kr.hhp227.groupsns_webapp.common.exception.DuplicateEmailException
import kr.hhp227.groupsns_webapp.common.exception.InvalidGoogleTokenException
import kr.hhp227.groupsns_webapp.group.Group
import kr.hhp227.groupsns_webapp.group.GroupMapper
import kr.hhp227.groupsns_webapp.group.GroupRole
import kr.hhp227.groupsns_webapp.group.UserGroupMapper
import kr.hhp227.groupsns_webapp.security.JwtTokenProvider
import kr.hhp227.groupsns_webapp.user.NewUserRecord
import kr.hhp227.groupsns_webapp.user.User
import kr.hhp227.groupsns_webapp.user.UserMapper
import kr.hhp227.groupsns_webapp.user.UserRole
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.OffsetDateTime

// 계정 결정 분기(설계 §2.3) — 전 의존성 mock, JwtTokenProvider만 실물(JwtTokenProviderTests 관례).
// Kotlin non-null 파라미터에 Mockito any()/capture()가 null을 돌려주는 문제는 `?: 더미` 관용구로 피한다.
class AuthServiceGoogleLoginTest {
    private val userMapper = Mockito.mock(UserMapper::class.java)
    private val refreshTokenMapper = Mockito.mock(RefreshTokenMapper::class.java)
    private val loginHistoryMapper = Mockito.mock(LoginHistoryMapper::class.java)
    private val groupMapper = Mockito.mock(GroupMapper::class.java)
    private val userGroupMapper = Mockito.mock(UserGroupMapper::class.java)
    private val oauthAccountMapper = Mockito.mock(OauthAccountMapper::class.java)
    private val googleTokenVerifier = Mockito.mock(GoogleTokenVerifier::class.java)
    private val googleCodeExchanger = Mockito.mock(GoogleCodeExchanger::class.java)
    private val googleAccessTokenVerifier = Mockito.mock(GoogleAccessTokenVerifier::class.java)
    private val jwt = JwtTokenProvider("test-secret-test-secret-test-secret-1234", 1_800_000)
    private val service = AuthService(
        userMapper, refreshTokenMapper, loginHistoryMapper, groupMapper, userGroupMapper,
        oauthAccountMapper, googleTokenVerifier, googleCodeExchanger, googleAccessTokenVerifier,
        Mockito.mock(AppleTokenVerifier::class.java), Mockito.mock(AppleCodeExchanger::class.java),
        AppleOAuthProperties("", "", "", "", "", "", ""), AppleLoginCodes("test-secret-test-secret-test-secret-1234", 60_000),
        Mockito.mock(PasswordEncoder::class.java), jwt, 1_209_600_000
    )
    private val dummyRecord = NewUserRecord("", "", null)

    init {
        // Mockito는 Long? 반환에 null이 아니라 0L을 준다 — MyBatis처럼 "연결 없음"=null을 기본으로 둔다
        Mockito.`when`(oauthAccountMapper.findUserId(Mockito.anyString(), Mockito.anyString())).thenReturn(null)
    }

    private fun identity(emailVerified: Boolean = true) =
        GoogleIdentity(sub = "g-123", email = "a@gmail.com", emailVerified = emailVerified, name = "구글이", picture = "https://pic")

    private fun user(id: Long = 7, email: String = "a@gmail.com") = User(
        id = id, name = "테스터", email = email, passwordHash = null, status = 0, role = UserRole.USER,
        profileImg = null, bio = null, statusMessage = null, createdAt = OffsetDateTime.now(), deletedAt = null
    )

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
    fun `이미 연결된 구글 계정이면 그 사용자로 로그인하고 연결을 새로 만들지 않는다`() {
        Mockito.`when`(googleTokenVerifier.verify("tok")).thenReturn(identity())
        Mockito.`when`(oauthAccountMapper.findUserId("GOOGLE", "g-123")).thenReturn(7L)
        Mockito.`when`(userMapper.findById(7)).thenReturn(user())

        val tokens = service.loginWithGoogleIdToken("tok", "1.1.1.1", "ua")

        assertEquals(7L, jwt.getUserId(tokens.accessToken))
        Mockito.verify(oauthAccountMapper, Mockito.never()).insert(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString())
        Mockito.verify(loginHistoryMapper).insert(7, "1.1.1.1", "ua", true)
    }

    @Test
    fun `연결된 사용자가 탈퇴했으면 401`() {
        Mockito.`when`(googleTokenVerifier.verify("tok")).thenReturn(identity())
        Mockito.`when`(oauthAccountMapper.findUserId("GOOGLE", "g-123")).thenReturn(7L)
        Mockito.`when`(userMapper.findById(7)).thenReturn(null)

        assertThrows(InvalidGoogleTokenException::class.java) { service.loginWithGoogleIdToken("tok", null, null) }
    }

    @Test
    fun `같은 이메일의 기존 계정이 있고 이메일이 검증됐으면 자동 연결한다`() {
        Mockito.`when`(googleTokenVerifier.verify("tok")).thenReturn(identity())
        Mockito.`when`(userMapper.findByEmail("a@gmail.com")).thenReturn(user(id = 9))

        val tokens = service.loginWithGoogleIdToken("tok", null, null)

        assertEquals(9L, jwt.getUserId(tokens.accessToken))
        Mockito.verify(oauthAccountMapper).insert(9, "GOOGLE", "g-123")
        Mockito.verify(userMapper, Mockito.never()).insert(any(NewUserRecord::class.java) ?: dummyRecord)
    }

    @Test
    fun `이메일 미검증인데 같은 이메일 계정이 있으면 409`() {
        Mockito.`when`(googleTokenVerifier.verify("tok")).thenReturn(identity(emailVerified = false))
        Mockito.`when`(userMapper.findByEmail("a@gmail.com")).thenReturn(user(id = 9))

        assertThrows(DuplicateEmailException::class.java) { service.loginWithGoogleIdToken("tok", null, null) }
        Mockito.verify(oauthAccountMapper, Mockito.never()).insert(Mockito.anyLong(), Mockito.anyString(), Mockito.anyString())
    }

    @Test
    fun `처음 보는 구글 계정이면 비밀번호 없이 가입시키고 라운지에 넣는다`() {
        Mockito.`when`(googleTokenVerifier.verify("tok")).thenReturn(identity())
        stubInsertAssigningId(11)
        Mockito.`when`(groupMapper.findLounge()).thenReturn(
            Group(1, 2, "라운지", null, null, 0, OffsetDateTime.now(), null, true)
        )

        val tokens = service.loginWithGoogleIdToken("tok", null, null)

        val record = capturedInsert()
        assertNull(record.passwordHash)
        assertEquals("구글이", record.name)
        assertEquals("https://pic", record.profileImg)
        Mockito.verify(oauthAccountMapper).insert(11, "GOOGLE", "g-123")
        Mockito.verify(userGroupMapper).insert(11, 1, GroupRole.MEMBER)
        assertEquals(11L, jwt.getUserId(tokens.accessToken))
    }

    @Test
    fun `이름이 없으면 이메일 앞부분을 이름으로 쓴다`() {
        Mockito.`when`(googleTokenVerifier.verify("tok")).thenReturn(identity().copy(name = null))
        stubInsertAssigningId(11)

        service.loginWithGoogleIdToken("tok", null, null)

        assertEquals("a", capturedInsert().name)
    }

    @Test
    fun `Desktop 코드는 교환한 id_token으로 같은 경로를 탄다`() {
        Mockito.`when`(googleCodeExchanger.exchange("c", "v", "http://127.0.0.1:5000")).thenReturn("tok")
        Mockito.`when`(googleTokenVerifier.verify("tok")).thenReturn(identity())
        Mockito.`when`(oauthAccountMapper.findUserId("GOOGLE", "g-123")).thenReturn(7L)
        Mockito.`when`(userMapper.findById(7)).thenReturn(user())

        val tokens = service.loginWithGoogleCode("c", "v", "http://127.0.0.1:5000", null, null)

        assertEquals(7L, jwt.getUserId(tokens.accessToken))
    }

    @Test
    fun `웹 액세스 토큰도 같은 계정 결정 경로를 탄다`() {
        Mockito.`when`(googleAccessTokenVerifier.verify("at")).thenReturn(identity())
        Mockito.`when`(userMapper.findByEmail("a@gmail.com")).thenReturn(user(id = 9))

        val tokens = service.loginWithGoogleAccessToken("at", null, null)

        assertEquals(9L, jwt.getUserId(tokens.accessToken))
        Mockito.verify(oauthAccountMapper).insert(9, "GOOGLE", "g-123")
    }
}
