package kr.hhp227.groupsns_webapp.realtime

import kr.hhp227.groupsns_webapp.push.FcmSender
import kr.hhp227.groupsns_webapp.push.PushContent
import kr.hhp227.groupsns_webapp.push.PushMessageFactory
import kr.hhp227.groupsns_webapp.push.PushTokenMapper
import kr.hhp227.groupsns_webapp.user.PushPreferences
import kr.hhp227.groupsns_webapp.user.UserMapper
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

// NotificationBroadcaster의 형제 — 같은 이벤트를 커밋 후에 받아 FCM으로도 내보낸다(푸시 설계 §3).
// 항상 발송 + 클라 억제 정책이라 프레즌스 게이트는 없고, 수신자의 종류별 on/off만 여기서 거른다
// (푸시 on/off 설계 §3 — 인앱 STOMP·뱃지는 NotificationBroadcaster가 따로 보내므로 영향 없음).
// RtcSocketEvent(CALL_INVITE)는 의도적으로 미구독 — VoIP 푸시는 후속 과제.
// 재시도 없음(fire-and-forget): 무효 토큰만 정리하고 일시 장애는 warn 로그.
@Component
class PushBroadcaster(
    // Kotlin 클래스는 final이라 테스트 mock이 안 됨 — 서비스가 아닌 매퍼 인터페이스를 직접 주입
    private val pushTokenMapper: PushTokenMapper,
    private val userMapper: UserMapper,
    private val fcmSender: FcmSender
) {
    private val log = LoggerFactory.getLogger(PushBroadcaster::class.java)

    @Async("pushTaskExecutor")
    @TransactionalEventListener
    fun on(event: NotificationSocketEvent) =
        push(event.recipientId, PushMessageFactory.from(event)) { it.activityEnabled }

    @Async("pushTaskExecutor")
    @TransactionalEventListener
    fun on(event: ChatBadgeSocketEvent) =
        push(event.recipientId, PushMessageFactory.from(event)) { it.chatEnabled }

    // 수신자 설정 게이트: 종류별 플래그가 꺼져 있으면 토큰 조회 전에 끝낸다. 설정 행이 없으면(탈퇴 등) 스킵.
    // @Async 스레드라 조회 1회 추가는 요청 지연과 무관, 조회 실패는 기존 예외 삼킴 경로를 탄다.
    private fun push(recipientId: Long, content: PushContent, enabled: (PushPreferences) -> Boolean) {
        try {
            val prefs = userMapper.findPushPreferences(recipientId) ?: return
            if (!enabled(prefs)) return
            val tokens = pushTokenMapper.findTokensByUser(recipientId)
            if (tokens.isEmpty()) return
            fcmSender.send(tokens, content).forEach { pushTokenMapper.deleteByToken(it) }
        } catch (e: Exception) {
            log.warn("푸시 발송 실패 recipient={}", recipientId, e)
        }
    }
}
