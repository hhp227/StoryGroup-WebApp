package kr.hhp227.groupsns_webapp.notification

import kr.hhp227.groupsns_webapp.block.UserBlockMapper
import kr.hhp227.groupsns_webapp.common.db.DbSessionMapper
import kr.hhp227.groupsns_webapp.common.exception.NotificationNotFoundException
import kr.hhp227.groupsns_webapp.notification.dto.NotificationResponse
import kr.hhp227.groupsns_webapp.notification.dto.UnreadCountResponse
import kr.hhp227.groupsns_webapp.realtime.NotificationSocketEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationService(
    private val notificationMapper: NotificationMapper,
    private val userBlockMapper: UserBlockMapper,
    private val dbSessionMapper: DbSessionMapper,
    // WS 개인 큐 전달은 NotificationBroadcaster가 커밋 후에 처리 — 여기선 이벤트 발행만.
    private val eventPublisher: ApplicationEventPublisher
) {
    @Transactional
    fun listNotifications(userId: Long, page: Int, size: Int): List<NotificationResponse> {
        dbSessionMapper.setCurrentUserId(userId)
        return notificationMapper.findByUser(userId, size, page * size).map { NotificationResponse.from(it) }
    }

    @Transactional
    fun countUnread(userId: Long): UnreadCountResponse {
        dbSessionMapper.setCurrentUserId(userId)
        return UnreadCountResponse(notificationMapper.countUnread(userId))
    }

    @Transactional
    fun markAsRead(userId: Long, notificationId: Long) {
        dbSessionMapper.setCurrentUserId(userId)
        val updated = notificationMapper.markAsRead(notificationId, userId)
        if (updated == 0) throw NotificationNotFoundException()
    }

    @Transactional
    fun markAllAsRead(userId: Long) {
        dbSessionMapper.setCurrentUserId(userId)
        notificationMapper.markAllAsRead(userId)
    }

    // 다른 도메인 서비스(Post/Comment/Like/Meeting 등)가 이벤트 발생 시 호출한다.
    // 이미 열려 있는 그 서비스의 트랜잭션에 합류하므로 setCurrentUserId를 다시 호출할 필요가 없다.
    // actorId(알림을 일으킨 사용자)를 넘기면 수신자가 그를 차단한 경우 알림을 만들지 않는다.
    @Transactional
    fun notify(
        recipientId: Long,
        type: NotificationType,
        targetType: NotificationTargetType?,
        targetId: Long?,
        actorId: Long? = null
    ) {
        if (actorId != null && userBlockMapper.exists(recipientId, actorId)) return
        val record = NewNotificationRecord(recipientId, type, targetType, targetId)
        notificationMapper.insert(record)
        notificationMapper.findById(record.id)?.let {
            eventPublisher.publishEvent(NotificationSocketEvent(recipientId, NotificationResponse.from(it)))
        }
    }

    @Transactional
    fun notifyAll(
        recipientIds: Collection<Long>,
        type: NotificationType,
        targetType: NotificationTargetType?,
        targetId: Long?,
        actorId: Long? = null
    ) {
        recipientIds.forEach { notify(it, type, targetType, targetId, actorId) }
    }
}
