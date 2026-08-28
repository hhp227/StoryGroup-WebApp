package kr.hhp227.groupsns_webapp.realtime

import kr.hhp227.groupsns_webapp.push.FcmSender
import kr.hhp227.groupsns_webapp.push.PushContent
import kr.hhp227.groupsns_webapp.push.PushMessageFactory
import kr.hhp227.groupsns_webapp.push.PushTokenMapper
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

// NotificationBroadcaster의 형제 — 같은 이벤트를 커밋 후에 받아 FCM으로도 내보낸다(설계 §3).
// 항상 발송 + 클라 억제 정책이라 프레즌스 게이트는 없다. RtcSocketEvent(CALL_INVITE)는
// 의도적으로 미구독 — VoIP 푸시는 후속 과제(설계 §12).
// 재시도 없음(fire-and-forget): 무효 토큰만 정리하고 일시 장애는 warn 로그.
@Component
class PushBroadcaster(
    // Kotlin 클래스는 final이라 테스트 mock이 안 됨 — 서비스가 아닌 매퍼 인터페이스를 직접 주입
    private val pushTokenMapper: PushTokenMapper,
    private val fcmSender: FcmSender
) {
    private val log = LoggerFactory.getLogger(PushBroadcaster::class.java)

    @Async("pushTaskExecutor")
    @TransactionalEventListener
    fun on(event: NotificationSocketEvent) = push(event.recipientId, PushMessageFactory.from(event))

    @Async("pushTaskExecutor")
    @TransactionalEventListener
    fun on(event: ChatBadgeSocketEvent) = push(event.recipientId, PushMessageFactory.from(event))

    private fun push(recipientId: Long, content: PushContent) {
        try {
            val tokens = pushTokenMapper.findTokensByUser(recipientId)
            if (tokens.isEmpty()) return
            fcmSender.send(tokens, content).forEach { pushTokenMapper.deleteByToken(it) }
        } catch (e: Exception) {
            log.warn("푸시 발송 실패 recipient={}", recipientId, e)
        }
    }
}
