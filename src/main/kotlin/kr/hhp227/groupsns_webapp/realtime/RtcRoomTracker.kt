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

// "지금 이 통화에 있는 사람" 추적 — RoomPresenceTracker의 rtc 토픽판(설계 문서 D3).
// rtc 토픽 구독 = 통화 입장으로 정의하고, 구독/해제/연결끊김을 인메모리 집계해
// 같은 토픽에 PEERS(전체 목록)를 방송한다. 단일 인스턴스 전제/지연 방송/세션 단위 추적 등
// 검증된 장치를 그대로 쓴다 — 차이는 방 키가 Long roomId가 아니라 roomKey 문자열이라는 것뿐.
// 구독 인가는 StompAuthChannelInterceptor가 SUBSCRIBE 시점에 이미 끝냈으므로 여기선 검사하지 않는다.
@Component
class RtcRoomTracker(
    private val broadcaster: RtcBroadcaster,
    @Qualifier("wsHeartbeatTaskScheduler") private val taskScheduler: ThreadPoolTaskScheduler
) {
    // sessionId -> (subscriptionId -> roomKey): UNSUBSCRIBE 프레임엔 subscriptionId만 실려 와서 필요.
    private val sessionSubs = mutableMapOf<String, MutableMap<String, String>>()

    // roomKey -> (sessionId -> peer): 같은 유저가 탭 2개로 들어와도 세션 단위로 추적하고,
    // 목록을 만들 때 userId로 중복 제거한다.
    private val rooms = mutableMapOf<String, MutableMap<String, RtcPeer>>()

    // 시그널 릴레이 전 "수신자가 같은 통화에 있는지" 검사용(RtcSignalController, D4).
    fun isInRoom(roomKey: String, userId: Long): Boolean = synchronized(this) {
        rooms[roomKey]?.values?.any { it.userId == userId } ?: false
    }

    @EventListener
    fun onSubscribe(event: SessionSubscribeEvent) {
        val accessor = StompHeaderAccessor.wrap(event.message)
        val sessionId = accessor.sessionId ?: return
        val subscriptionId = accessor.subscriptionId ?: return
        val roomKey = TOPIC_PATTERN.matchEntire(accessor.destination ?: return)
            ?.groupValues?.get(1) ?: return
        val principal = (event.user as? UsernamePasswordAuthenticationToken)?.principal as? UserPrincipal ?: return
        synchronized(this) {
            sessionSubs.getOrPut(sessionId) { mutableMapOf() }[subscriptionId] = roomKey
            rooms.getOrPut(roomKey) { mutableMapOf() }[sessionId] = RtcPeer(principal.id, principal.name)
        }
        scheduleBroadcast(roomKey)
    }

    @EventListener
    fun onUnsubscribe(event: SessionUnsubscribeEvent) {
        val accessor = StompHeaderAccessor.wrap(event.message)
        val sessionId = accessor.sessionId ?: return
        val subscriptionId = accessor.subscriptionId ?: return
        val roomKey = synchronized(this) {
            val subs = sessionSubs[sessionId] ?: return
            val roomKey = subs.remove(subscriptionId) ?: return
            // 같은 세션이 같은 방을 중복 구독한 경우, 마지막 구독이 풀릴 때만 목록에서 뺀다.
            if (subs.containsValue(roomKey)) return
            rooms[roomKey]?.remove(sessionId)
            roomKey
        }
        scheduleBroadcast(roomKey)
    }

    @EventListener
    fun onDisconnect(event: SessionDisconnectEvent) {
        // DISCONNECT 프레임과 전송 종료로 이벤트가 두 번 올 수 있다 — remove가 null이면 이미 처리된 것.
        val roomKeys = synchronized(this) {
            val subs = sessionSubs.remove(event.sessionId) ?: return
            subs.values.toSet().onEach { rooms[it]?.remove(event.sessionId) }
        }
        roomKeys.forEach { scheduleBroadcast(it) }
    }

    // SUBSCRIBE 처리(브로커의 구독 등록)와 이 이벤트 리스너는 다른 스레드에서 돌 수 있어,
    // 즉시 방송하면 방금 구독한 본인이 첫 PEERS를 놓칠 수 있다 — 짧게 늦춰 등록 이후에 나가게 한다.
    private fun scheduleBroadcast(roomKey: String) {
        taskScheduler.schedule({
            val peers = synchronized(this) {
                rooms[roomKey]?.values?.distinctBy { it.userId } ?: emptyList()
            }
            broadcaster.relayPeers(roomKey, peers)
        }, Instant.now().plusMillis(BROADCAST_DELAY_MS))
    }

    companion object {
        private const val BROADCAST_DELAY_MS = 200L
        private val TOPIC_PATTERN = Regex("""/topic/rtc/((?:meetings|chat-rooms)/\d+)""")
    }
}
