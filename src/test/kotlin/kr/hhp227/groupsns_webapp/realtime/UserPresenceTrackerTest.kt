package kr.hhp227.groupsns_webapp.realtime

import kr.hhp227.groupsns_webapp.friend.UserFriendMapper
import kr.hhp227.groupsns_webapp.security.UserPrincipal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageBuilder
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import org.springframework.web.socket.messaging.SessionSubscribeEvent
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent
import java.time.Instant

// 리스너 메서드 직접 호출 + Mockito(스케줄러의 Runnable을 캡처해 동기 실행)로 유예 로직까지 검증.
// Kotlin에서 Mockito 매처는 Java 메서드(SimpMessagingTemplate/ThreadPoolTaskScheduler) 대상이라 null 반환이 무해하다.
class UserPresenceTrackerTest {
    private val messagingTemplate = Mockito.mock(SimpMessagingTemplate::class.java)
    private val userFriendMapper = Mockito.mock(UserFriendMapper::class.java)
    private val taskScheduler = Mockito.mock(ThreadPoolTaskScheduler::class.java)
    private val tracker = UserPresenceTracker(messagingTemplate, userFriendMapper, taskScheduler)

    @Test
    fun `구독하면 팔로워 전원에게 온라인을 한 번만 발행한다`() {
        Mockito.`when`(userFriendMapper.findFollowerIds(1L)).thenReturn(listOf(2L, 3L))

        tracker.onSubscribe(subscribeEvent("s1", "sub-0", userId = 1L))
        // 두 번째 기기(세션) — 이미 온라인이라 추가 발행 없음
        tracker.onSubscribe(subscribeEvent("s2", "sub-0", userId = 1L))

        val payload = ArgumentCaptor.forClass(Any::class.java)
        Mockito.verify(messagingTemplate)
            .convertAndSendToUser(Mockito.eq("2"), Mockito.eq("/queue/notifications"), payload.capture())
        Mockito.verify(messagingTemplate)
            .convertAndSendToUser(Mockito.eq("3"), Mockito.eq("/queue/notifications"), Mockito.any())
        val event = payload.value as PresenceSocketEvent
        assertEquals(1L, event.userId)
        assertTrue(event.online)
        assertEquals("PRESENCE_CHANGED", event.type)
        assertTrue(tracker.isOnline(1L))
    }

    @Test
    fun `마지막 세션이 끊기면 즉시가 아니라 유예 재검사 후에 오프라인을 발행한다`() {
        Mockito.`when`(userFriendMapper.findFollowerIds(1L)).thenReturn(listOf(2L))
        tracker.onSubscribe(subscribeEvent("s1", "sub-0", userId = 1L))
        Mockito.clearInvocations(messagingTemplate)

        tracker.onDisconnect(disconnectEvent("s1"))

        Mockito.verifyNoInteractions(messagingTemplate) // 즉시 발행 없음 — 유예 태스크만
        val runnable = ArgumentCaptor.forClass(Runnable::class.java)
        Mockito.verify(taskScheduler).schedule(runnable.capture(), Mockito.any(Instant::class.java))
        runnable.value.run() // 유예 재검사 — 여전히 세션 0

        val payload = ArgumentCaptor.forClass(Any::class.java)
        Mockito.verify(messagingTemplate)
            .convertAndSendToUser(Mockito.eq("2"), Mockito.eq("/queue/notifications"), payload.capture())
        assertFalse((payload.value as PresenceSocketEvent).online)
        assertFalse(tracker.isOnline(1L))
    }

    @Test
    fun `유예 안에 다시 접속하면 오프라인도 온라인도 발행하지 않는다`() {
        Mockito.`when`(userFriendMapper.findFollowerIds(1L)).thenReturn(listOf(2L))
        tracker.onSubscribe(subscribeEvent("s1", "sub-0", userId = 1L))
        Mockito.clearInvocations(messagingTemplate)

        tracker.onDisconnect(disconnectEvent("s1"))
        val runnable = ArgumentCaptor.forClass(Runnable::class.java)
        Mockito.verify(taskScheduler).schedule(runnable.capture(), Mockito.any(Instant::class.java))
        tracker.onSubscribe(subscribeEvent("s2", "sub-0", userId = 1L)) // 유예 내 재연결
        runnable.value.run() // 재검사 — 세션이 살아 있음

        Mockito.verifyNoInteractions(messagingTemplate) // 상태 연속(친구들은 끊김을 못 본다)
        assertTrue(tracker.isOnline(1L))
    }

    @Test
    fun `구독 해제도 마지막 세션이면 유예 후 오프라인을 발행한다`() {
        Mockito.`when`(userFriendMapper.findFollowerIds(1L)).thenReturn(listOf(2L))
        tracker.onSubscribe(subscribeEvent("s1", "sub-0", userId = 1L))
        Mockito.clearInvocations(messagingTemplate)

        tracker.onUnsubscribe(unsubscribeEvent("s1", "sub-0"))
        val runnable = ArgumentCaptor.forClass(Runnable::class.java)
        Mockito.verify(taskScheduler).schedule(runnable.capture(), Mockito.any(Instant::class.java))
        runnable.value.run()

        Mockito.verify(messagingTemplate)
            .convertAndSendToUser(Mockito.eq("2"), Mockito.eq("/queue/notifications"), Mockito.any())
    }

    @Test
    fun `개인 큐가 아닌 destination 구독은 무시한다`() {
        tracker.onSubscribe(subscribeEvent("s1", "sub-0", userId = 1L, destination = "/topic/chat-rooms/5"))

        Mockito.verifyNoInteractions(messagingTemplate)
        assertFalse(tracker.isOnline(1L))
    }

    private fun subscribeEvent(
        sessionId: String,
        subscriptionId: String,
        userId: Long,
        destination: String = "/user/queue/notifications"
    ): SessionSubscribeEvent {
        val accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE)
        accessor.sessionId = sessionId
        accessor.subscriptionId = subscriptionId
        accessor.destination = destination
        val user = UsernamePasswordAuthenticationToken(
            UserPrincipal(userId, "유저$userId", "u$userId@test.local"), null, emptyList()
        )
        return SessionSubscribeEvent(this, MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders), user)
    }

    private fun unsubscribeEvent(sessionId: String, subscriptionId: String): SessionUnsubscribeEvent {
        val accessor = StompHeaderAccessor.create(StompCommand.UNSUBSCRIBE)
        accessor.sessionId = sessionId
        accessor.subscriptionId = subscriptionId
        return SessionUnsubscribeEvent(this, MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders))
    }

    private fun disconnectEvent(sessionId: String): SessionDisconnectEvent {
        val accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT)
        accessor.sessionId = sessionId
        return SessionDisconnectEvent(
            this, MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders), sessionId, CloseStatus.NORMAL
        )
    }
}
