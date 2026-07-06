package kr.hhp227.groupsns_webapp.realtime

import kr.hhp227.groupsns_webapp.security.UserPrincipal
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import org.springframework.web.socket.messaging.SessionSubscribeEvent
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent
import java.time.Instant

// "지금 이 방을 보고 있는 사람" 추적(설계 문서 §7 Presence). 방 토픽 구독 = 보는 중으로 정의하고,
// 구독/해제/연결끊김 이벤트를 인메모리로 집계해 같은 토픽에 PRESENCE(전체 목록)를 방송한다.
// 인메모리 단일 인스턴스 전제(D2) — 다중 인스턴스로 가면 ChatEventBroadcaster 교체와 묶어 재설계.
// 구독 인가는 StompAuthChannelInterceptor가 SUBSCRIBE 시점에 이미 끝냈으므로 여기선 검사하지 않는다.
@Component
class RoomPresenceTracker(
    private val broadcaster: ChatEventBroadcaster,
    // Spring 내장 messageBrokerTaskScheduler와 타입이 겹치므로 우리 빈을 이름으로 지정한다.
    @Qualifier("wsHeartbeatTaskScheduler") private val taskScheduler: ThreadPoolTaskScheduler
) {
    // sessionId -> (subscriptionId -> roomId): UNSUBSCRIBE 프레임엔 subscriptionId만 실려 와서 필요.
    private val sessionSubs = mutableMapOf<String, MutableMap<String, Long>>()

    // roomId -> (sessionId -> user): 같은 유저가 탭 2개로 들어와도 세션 단위로 추적하고,
    // 목록을 만들 때 userId로 중복 제거한다.
    private val rooms = mutableMapOf<Long, MutableMap<String, PresenceUser>>()

    @EventListener
    fun onSubscribe(event: SessionSubscribeEvent) {
        val accessor = StompHeaderAccessor.wrap(event.message)
        val sessionId = accessor.sessionId ?: return
        val subscriptionId = accessor.subscriptionId ?: return
        val roomId = TOPIC_PATTERN.matchEntire(accessor.destination ?: return)
            ?.groupValues?.get(1)?.toLongOrNull() ?: return
        val principal = (event.user as? UsernamePasswordAuthenticationToken)?.principal as? UserPrincipal ?: return
        synchronized(this) {
            sessionSubs.getOrPut(sessionId) { mutableMapOf() }[subscriptionId] = roomId
            rooms.getOrPut(roomId) { mutableMapOf() }[sessionId] = PresenceUser(principal.id, principal.name)
        }
        scheduleBroadcast(roomId)
    }

    @EventListener
    fun onUnsubscribe(event: SessionUnsubscribeEvent) {
        val accessor = StompHeaderAccessor.wrap(event.message)
        val sessionId = accessor.sessionId ?: return
        val subscriptionId = accessor.subscriptionId ?: return
        val roomId = synchronized(this) {
            val subs = sessionSubs[sessionId] ?: return
            val roomId = subs.remove(subscriptionId) ?: return
            // 같은 세션이 같은 방을 중복 구독한 경우, 마지막 구독이 풀릴 때만 목록에서 뺀다.
            if (subs.containsValue(roomId)) return
            rooms[roomId]?.remove(sessionId)
            roomId
        }
        scheduleBroadcast(roomId)
    }

    @EventListener
    fun onDisconnect(event: SessionDisconnectEvent) {
        // DISCONNECT 프레임과 전송 종료로 이벤트가 두 번 올 수 있다 — remove가 null이면 이미 처리된 것.
        val roomIds = synchronized(this) {
            val subs = sessionSubs.remove(event.sessionId) ?: return
            subs.values.toSet().onEach { rooms[it]?.remove(event.sessionId) }
        }
        roomIds.forEach { scheduleBroadcast(it) }
    }

    // SUBSCRIBE 처리(브로커의 구독 등록)와 이 이벤트 리스너는 다른 스레드에서 돌 수 있어,
    // 즉시 방송하면 방금 구독한 본인이 첫 PRESENCE를 놓칠 수 있다 — 짧게 늦춰 등록 이후에 나가게 한다.
    private fun scheduleBroadcast(roomId: Long) {
        taskScheduler.schedule({
            val users = synchronized(this) {
                rooms[roomId]?.values?.distinctBy { it.userId } ?: emptyList()
            }
            broadcaster.relay(ChatSocketEvent.presence(roomId, users))
        }, Instant.now().plusMillis(BROADCAST_DELAY_MS))
    }

    companion object {
        private const val BROADCAST_DELAY_MS = 200L
        private val TOPIC_PATTERN = Regex("""/topic/chat-rooms/(\d+)""")
    }
}
