package kr.hhp227.groupsns_webapp.user

import kr.hhp227.groupsns_webapp.auth.RefreshTokenMapper
import kr.hhp227.groupsns_webapp.common.exception.UserNotFoundException
import kr.hhp227.groupsns_webapp.friend.UserFriendMapper
import kr.hhp227.groupsns_webapp.group.UserGroupMapper
import kr.hhp227.groupsns_webapp.push.PushTokenMapper
import kr.hhp227.groupsns_webapp.user.dto.UpdatePushPreferencesRequest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.security.crypto.password.PasswordEncoder

// UserServiceDeleteAccountTest와 같은 방식 — 생성자 전 의존성 mock, JUnit5+Mockito 직접 호출
class UserServicePushPreferencesTest {
    private val userMapper = Mockito.mock(UserMapper::class.java)
    private val service = UserService(
        userMapper,
        Mockito.mock(UserGroupMapper::class.java),
        Mockito.mock(RefreshTokenMapper::class.java),
        Mockito.mock(PushTokenMapper::class.java),
        Mockito.mock(UserFriendMapper::class.java),
        Mockito.mock(PasswordEncoder::class.java)
    )

    @Test
    fun `조회는 매퍼 행을 응답 DTO로 옮긴다`() {
        Mockito.`when`(userMapper.findPushPreferences(7)).thenReturn(PushPreferences(chatEnabled = false, activityEnabled = true))
        val response = service.getPushPreferences(7)
        assertFalse(response.chatEnabled)
        assertTrue(response.activityEnabled)
    }

    @Test
    fun `조회 - 유저가 없으면 UserNotFound`() {
        Mockito.`when`(userMapper.findPushPreferences(7)).thenReturn(null)
        assertThrows(UserNotFoundException::class.java) { service.getPushPreferences(7) }
    }

    @Test
    fun `갱신은 요청 값 두 개를 그대로 매퍼에 넘긴다`() {
        Mockito.`when`(userMapper.updatePushPreferences(7, false, true)).thenReturn(1)
        service.updatePushPreferences(7, UpdatePushPreferencesRequest(chatEnabled = false, activityEnabled = true))
        Mockito.verify(userMapper).updatePushPreferences(7, false, true)
    }

    @Test
    fun `갱신 - 영향 행 0이면 UserNotFound`() {
        Mockito.`when`(userMapper.updatePushPreferences(7, true, true)).thenReturn(0)
        assertThrows(UserNotFoundException::class.java) {
            service.updatePushPreferences(7, UpdatePushPreferencesRequest(chatEnabled = true, activityEnabled = true))
        }
    }

    @Test
    fun `갱신 - null 플래그는 IllegalArgumentException(직접 호출 방어)`() {
        assertThrows(IllegalArgumentException::class.java) {
            service.updatePushPreferences(7, UpdatePushPreferencesRequest(chatEnabled = null, activityEnabled = true))
        }
        Mockito.verifyNoInteractions(userMapper)
    }
}
