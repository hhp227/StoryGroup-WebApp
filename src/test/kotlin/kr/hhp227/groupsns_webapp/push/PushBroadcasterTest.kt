package kr.hhp227.groupsns_webapp.push

import kr.hhp227.groupsns_webapp.notification.NotificationTargetType
import kr.hhp227.groupsns_webapp.notification.NotificationType
import kr.hhp227.groupsns_webapp.notification.dto.NotificationResponse
import kr.hhp227.groupsns_webapp.realtime.ChatBadgeSocketEvent
import kr.hhp227.groupsns_webapp.realtime.NotificationSocketEvent
import kr.hhp227.groupsns_webapp.realtime.PushBroadcaster
import kr.hhp227.groupsns_webapp.user.PushPreferences
import kr.hhp227.groupsns_webapp.user.UserMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.time.OffsetDateTime

class PushBroadcasterTest {
    private class FakeFcmSender(private val invalid: List<String> = emptyList()) : FcmSender {
        var sentTokens: List<String>? = null
        var sentContent: PushContent? = null
        override fun send(tokens: List<String>, content: PushContent): List<String> {
            sentTokens = tokens
            sentContent = content
            return invalid
        }
    }

    private val mapper = Mockito.mock(PushTokenMapper::class.java)
    private val userMapper = Mockito.mock(UserMapper::class.java)

    private fun broadcaster(sender: FcmSender) = PushBroadcaster(mapper, userMapper, sender)

    private fun stubPrefs(userId: Long, chat: Boolean, activity: Boolean) {
        Mockito.`when`(userMapper.findPushPreferences(userId)).thenReturn(PushPreferences(chatEnabled = chat, activityEnabled = activity))
    }

    private fun chatEvent(recipientId: Long) = ChatBadgeSocketEvent(
        recipientId = recipientId, chatRoomId = 5, messageId = 10, senderId = 2,
        senderName = "홍길동", roomName = null, groupId = null, text = "안녕"
    )

    // PushMessageFactoryTest의 빌더와 같은 형태 — 라벨 검증은 저쪽 몫, 여기선 게이트 분기만 본다
    private fun notificationEvent(recipientId: Long) = NotificationSocketEvent(
        recipientId = recipientId,
        notification = NotificationResponse(
            id = 42, type = NotificationType.COMMENT, targetType = NotificationTargetType.POST, targetId = 7,
            isRead = false, createdAt = OffsetDateTime.now(),
            postId = 7, postPreview = "미리보기", groupId = 3, groupName = "우리모임"
        )
    )

    @Test
    fun `토큰이 없으면 발송하지 않는다`() {
        stubPrefs(1, chat = true, activity = true)
        Mockito.`when`(mapper.findTokensByUser(1)).thenReturn(emptyList())
        val sender = FakeFcmSender()
        broadcaster(sender).on(chatEvent(1))
        assertNull(sender.sentTokens)
    }

    @Test
    fun `수신자의 전 토큰으로 발송하고 무효 토큰은 삭제한다`() {
        stubPrefs(1, chat = true, activity = true)
        Mockito.`when`(mapper.findTokensByUser(1)).thenReturn(listOf("tokA", "tokB"))
        val sender = FakeFcmSender(invalid = listOf("tokB"))
        broadcaster(sender).on(chatEvent(1))
        assertEquals(listOf("tokA", "tokB"), sender.sentTokens)
        assertEquals("홍길동", sender.sentContent!!.title)
        Mockito.verify(mapper).deleteByToken("tokB")
        Mockito.verify(mapper, Mockito.never()).deleteByToken("tokA")
    }

    @Test
    fun `발송 예외는 삼킨다 - 리스너가 죽으면 안 된다`() {
        stubPrefs(1, chat = true, activity = true)
        Mockito.`when`(mapper.findTokensByUser(1)).thenReturn(listOf("tokA"))
        val throwing = object : FcmSender {
            override fun send(tokens: List<String>, content: PushContent): List<String> = throw RuntimeException("fcm down")
        }
        broadcaster(throwing).on(chatEvent(1)) // 예외가 전파되면 테스트 실패
    }

    @Test
    fun `채팅 푸시 OFF면 채팅 이벤트는 토큰 조회조차 하지 않는다`() {
        stubPrefs(1, chat = false, activity = true)
        val sender = FakeFcmSender()
        broadcaster(sender).on(chatEvent(1))
        assertNull(sender.sentTokens)
        Mockito.verify(mapper, Mockito.never()).findTokensByUser(1)
    }

    @Test
    fun `채팅 푸시 ON이면 활동 푸시가 OFF여도 채팅 이벤트는 발송한다`() {
        stubPrefs(1, chat = true, activity = false)
        Mockito.`when`(mapper.findTokensByUser(1)).thenReturn(listOf("tokA"))
        val sender = FakeFcmSender()
        broadcaster(sender).on(chatEvent(1))
        assertEquals(listOf("tokA"), sender.sentTokens)
    }

    @Test
    fun `활동 푸시 OFF면 알림 이벤트는 토큰 조회조차 하지 않는다`() {
        stubPrefs(1, chat = true, activity = false)
        val sender = FakeFcmSender()
        broadcaster(sender).on(notificationEvent(1))
        assertNull(sender.sentTokens)
        Mockito.verify(mapper, Mockito.never()).findTokensByUser(1)
    }

    @Test
    fun `활동 푸시 ON이면 채팅 푸시가 OFF여도 알림 이벤트는 발송한다`() {
        stubPrefs(1, chat = false, activity = true)
        Mockito.`when`(mapper.findTokensByUser(1)).thenReturn(listOf("tokA"))
        val sender = FakeFcmSender()
        broadcaster(sender).on(notificationEvent(1))
        assertEquals(listOf("tokA"), sender.sentTokens)
    }

    @Test
    fun `설정 행이 없으면(탈퇴 등 유저 부재) 발송하지 않는다`() {
        Mockito.`when`(userMapper.findPushPreferences(1)).thenReturn(null)
        val sender = FakeFcmSender()
        broadcaster(sender).on(chatEvent(1))
        assertNull(sender.sentTokens)
        Mockito.verify(mapper, Mockito.never()).findTokensByUser(1)
    }
}
