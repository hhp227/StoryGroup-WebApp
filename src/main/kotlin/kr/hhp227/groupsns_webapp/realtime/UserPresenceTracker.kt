package kr.hhp227.groupsns_webapp.realtime

import kr.hhp227.groupsns_webapp.friend.UserFriendMapper
import kr.hhp227.groupsns_webapp.security.UserPrincipal
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.event.EventListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.socket.messaging.SessionDisconnectEvent
import org.springframework.web.socket.messaging.SessionSubscribeEvent
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent
import java.time.Instant

// 전역 프레즌스 — "개인 알림 큐 구독 중 = 온라인"으로 정의한다(RoomPresenceTracker의 전역판).
// KMP·웹 모두 이 큐를 전역 상시 구독하므로 클라 연결 변경 없이 성립한다.
// 인메모리 단일 인스턴스 전제(D2) — 다중 인스턴스로 가면 ChatEventBroadcaster 교체와 묶어 재설계.
// 서버 재시작이면 전원 오프라인에서 시작 — 클라 재구독이 다시 쌓고, 친구 목록 스냅샷이 복구 경로다.
@Component
class UserPresenceTracker(
    private val messagingTemplate: SimpMessagingTemplate,
    private val userFriendMapper: UserFriendMapper,
    // Spring 내장 messageBrokerTaskScheduler와 타입이 겹치므로 우리 빈을 이름으로 지정한다.
    @Qualifier("wsHeartbeatTaskScheduler") private val taskScheduler: ThreadPoolTaskScheduler
) {
    // sessionId -> 개인 큐 subscriptionId 집합: UNSUBSCRIBE 프레임엔 subscriptionId만 실려 와서 필요.
    private val sessionSubs = mutableMapOf<String, MutableSet<String>>()

    // sessionId -> userId: DISCONNECT 프레임엔 principal이 없을 수 있어 역추적용으로 보관.
    private val sessionUser = mutableMapOf<String, Long>()

    // userId -> sessionId 집합: 같은 유저가 기기 2개로 접속해도 세션 단위로 추적한다.
    private val userSessions = mutableMapOf<Long, MutableSet<String>>()

    // 마지막으로 발행한 온라인 유저 집합 — 실제 전환에만 발행한다(유예 내 재접속 시 재발행 억제).
    private val broadcastOnline = mutableSetOf<Long>()

    // 스냅샷은 세션 유무가 아니라 "발행된 상태"를 읽는다 — 유예 창(세션 0이지만 오프라인 미발행)에
    // 조회한 팔로워가 오프라인 스냅샷을 받고, 그 뒤 유예 내 재접속으로 어떤 발행도 안 나가면
    // 영구히 오프라인으로 남는 불일치를 막는다. 유예 뒤 진짜 오프라인이면 발행이 스냅샷 보유자에게 도달해 수렴한다.
    fun isOnline(userId: Long): Boolean = synchronized(this) { userId in broadcastOnline }

    @EventListener
    fun onSubscribe(event: SessionSubscribeEvent) {
        val accessor = StompHeaderAccessor.wrap(event.message)
        val sessionId = accessor.sessionId ?: return
        val subscriptionId = accessor.subscriptionId ?: return
        if (accessor.destination != PRESENCE_DESTINATION) return
        val principal = (event.user as? UsernamePasswordAuthenticationToken)?.principal as? UserPrincipal ?: return
        val becameOnline = synchronized(this) {
            sessionSubs.getOrPut(sessionId) { mutableSetOf() }.add(subscriptionId)
            sessionUser[sessionId] = principal.id
            userSessions.getOrPut(principal.id) { mutableSetOf() }.add(sessionId)
            broadcastOnline.add(principal.id)
        }
        if (becameOnline) publish(principal.id, online = true)
    }

    @EventListener
    fun onUnsubscribe(event: SessionUnsubscribeEvent) {
        val accessor = StompHeaderAccessor.wrap(event.message)
        val sessionId = accessor.sessionId ?: return
        val subscriptionId = accessor.subscriptionId ?: return
        val offlineCandidate = synchronized(this) {
            val subs = sessionSubs[sessionId] ?: return
            // 다른 destination(채팅방 토픽 등)의 구독 해제는 우리 집합에 없다 — 무시.
            if (!subs.remove(subscriptionId)) return
            // 같은 세션이 개인 큐를 중복 구독한 경우, 마지막 구독이 풀릴 때만 세션을 뺀다.
            if (subs.isNotEmpty()) return
            sessionSubs.remove(sessionId)
            val userId = sessionUser.remove(sessionId) ?: return
            removeSession(userId, sessionId)
        } ?: return
        scheduleOfflineCheck(offlineCandidate)
    }

    @EventListener
    fun onDisconnect(event: SessionDisconnectEvent) {
        // DISCONNECT 프레임과 전송 종료로 이벤트가 두 번 올 수 있다 — remove가 null이면 이미 처리된 것.
        val offlineCandidate = synchronized(this) {
            sessionSubs.remove(event.sessionId)
            val userId = sessionUser.remove(event.sessionId) ?: return
            removeSession(userId, event.sessionId)
        } ?: return
        scheduleOfflineCheck(offlineCandidate)
    }

    // userSessions에서 세션을 빼고, 그 유저의 마지막 세션이었으면 userId를 돌려준다(오프라인 후보).
    private fun removeSession(userId: Long, sessionId: String): Long? {
        val sessions = userSessions[userId] ?: return null
        sessions.remove(sessionId)
        if (sessions.isNotEmpty()) return null
        userSessions.remove(userId)
        return userId
    }

    // 마지막 세션이 사라져도 곧바로 오프라인을 알리지 않는다 — 클라 STOMP 재연결 주기(5초)의 2배를
    // 기다렸다가 재검사해, 네트워크 순단으로 온/오프라인이 튀는 것을 흡수한다(취소 관리 없이 재검사만).
    private fun scheduleOfflineCheck(userId: Long) {
        taskScheduler.schedule({
            val wentOffline = synchronized(this) {
                if (userSessions[userId]?.isNotEmpty() == true) false
                else broadcastOnline.remove(userId)
            }
            if (wentOffline) publish(userId, online = false)
        }, Instant.now().plusMillis(OFFLINE_GRACE_MS))
    }

    // 트랜잭션이 없는 휘발성 이벤트라 @TransactionalEventListener 경유 금지(조용히 버려짐) —
    // ChatEventBroadcaster.relay()처럼 직접 발행한다. 수신 대상은 "이 유저를 친구로 등록한 사람들".
    private fun publish(userId: Long, online: Boolean) {
        userFriendMapper.findFollowerIds(userId).forEach { followerId ->
            messagingTemplate.convertAndSendToUser(
                followerId.toString(), "/queue/notifications", PresenceSocketEvent(userId, online)
            )
        }
    }

    companion object {
        private const val OFFLINE_GRACE_MS = 10_000L
        private const val PRESENCE_DESTINATION = "/user/queue/notifications"
    }
}
