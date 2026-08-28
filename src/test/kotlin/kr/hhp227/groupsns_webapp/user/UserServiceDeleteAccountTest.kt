package kr.hhp227.groupsns_webapp.user

import kr.hhp227.groupsns_webapp.auth.RefreshTokenMapper
import kr.hhp227.groupsns_webapp.common.exception.OwnedGroupsExistException
import kr.hhp227.groupsns_webapp.friend.UserFriendMapper
import kr.hhp227.groupsns_webapp.group.UserGroupMapper
import kr.hhp227.groupsns_webapp.push.PushTokenMapper
import kr.hhp227.groupsns_webapp.user.dto.DeleteAccountRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.OffsetDateTime

// UserService 생성자 시그니처(userMapper, userGroupMapper, refreshTokenMapper, pushTokenMapper,
// userFriendMapper, passwordEncoder)에 맞춰 전 의존성 mock. JUnit5+Mockito 직접 호출(UserPresenceTrackerTest 관례 미러).
class UserServiceDeleteAccountTest {
    private val userMapper = Mockito.mock(UserMapper::class.java)
    private val userGroupMapper = Mockito.mock(UserGroupMapper::class.java)
    private val refreshTokenMapper = Mockito.mock(RefreshTokenMapper::class.java)
    private val pushTokenMapper = Mockito.mock(PushTokenMapper::class.java)
    private val userFriendMapper = Mockito.mock(UserFriendMapper::class.java)
    private val passwordEncoder = Mockito.mock(PasswordEncoder::class.java)
    private val service = UserService(userMapper, userGroupMapper, refreshTokenMapper, pushTokenMapper, userFriendMapper, passwordEncoder)

    private fun user(passwordHash: String? = "\$2a\$10\$hash") = User(
        id = 7,
        name = "테스터",
        email = "tester@example.com",
        passwordHash = passwordHash,
        status = 1,
        role = UserRole.USER,
        profileImg = null,
        bio = null,
        statusMessage = null,
        createdAt = OffsetDateTime.now(),
        deletedAt = null
    )

    @Test
    fun `비밀번호 불일치면 400 예외, 아무것도 바꾸지 않는다`() {
        Mockito.`when`(userMapper.findById(7)).thenReturn(user())
        Mockito.`when`(passwordEncoder.matches("wrong", "\$2a\$10\$hash")).thenReturn(false)
        assertThrows(IllegalArgumentException::class.java) { service.deleteAccount(7, DeleteAccountRequest("wrong")) }
        // findOwnedGroupNames 이전에 던지므로 userGroupMapper도 완전 미상호작용
        Mockito.verifyNoInteractions(refreshTokenMapper, pushTokenMapper, userFriendMapper, userGroupMapper)
        Mockito.verify(userMapper, Mockito.never()).anonymize(7)
        Mockito.verify(userMapper, Mockito.never()).deleteOauthAccounts(7)
    }

    @Test
    fun `password_hash가 NULL이면 400 예외`() {
        Mockito.`when`(userMapper.findById(7)).thenReturn(user(passwordHash = null))
        assertThrows(IllegalArgumentException::class.java) { service.deleteAccount(7, DeleteAccountRequest("any")) }
        // matches 호출 전에 던지므로 정리 6종 전부 미실행
        Mockito.verifyNoInteractions(refreshTokenMapper, pushTokenMapper, userFriendMapper, userGroupMapper)
        Mockito.verify(userMapper, Mockito.never()).anonymize(7)
        Mockito.verify(userMapper, Mockito.never()).deleteOauthAccounts(7)
    }

    @Test
    fun `OWNER 그룹이 있으면 409 예외에 그룹명이 실리고 익명화하지 않는다`() {
        Mockito.`when`(userMapper.findById(7)).thenReturn(user())
        Mockito.`when`(passwordEncoder.matches("pw", "\$2a\$10\$hash")).thenReturn(true)
        Mockito.`when`(userGroupMapper.findOwnedGroupNames(7)).thenReturn(listOf("우리모임", "스터디"))
        val ex = assertThrows(OwnedGroupsExistException::class.java) { service.deleteAccount(7, DeleteAccountRequest("pw")) }
        assertEquals("'우리모임, 스터디' 그룹을 삭제한 후 탈퇴할 수 있습니다", ex.message)
        // userGroupMapper는 findOwnedGroupNames로 이미 상호작용했으므로 verifyNoInteractions 대상에서 제외하고
        // deleteAllForUser 미호출만 개별 검증
        Mockito.verifyNoInteractions(refreshTokenMapper, pushTokenMapper, userFriendMapper)
        Mockito.verify(userGroupMapper, Mockito.never()).deleteAllForUser(7)
        Mockito.verify(userMapper, Mockito.never()).anonymize(7)
        Mockito.verify(userMapper, Mockito.never()).deleteOauthAccounts(7)
    }

    @Test
    fun `정상 탈퇴 - 익명화와 연관 정리를 전부 수행한다`() {
        Mockito.`when`(userMapper.findById(7)).thenReturn(user())
        Mockito.`when`(passwordEncoder.matches("pw", "\$2a\$10\$hash")).thenReturn(true)
        Mockito.`when`(userGroupMapper.findOwnedGroupNames(7)).thenReturn(emptyList())
        service.deleteAccount(7, DeleteAccountRequest("pw"))
        Mockito.verify(userMapper).anonymize(7)
        Mockito.verify(refreshTokenMapper).revokeAllForUser(7)
        Mockito.verify(pushTokenMapper).deleteAllForUser(7)
        Mockito.verify(userGroupMapper).deleteAllForUser(7)
        Mockito.verify(userFriendMapper).deleteAllInvolving(7)
        Mockito.verify(userMapper).deleteOauthAccounts(7)
    }
}
