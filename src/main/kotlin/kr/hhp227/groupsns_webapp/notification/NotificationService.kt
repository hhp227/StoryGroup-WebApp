package kr.hhp227.groupsns_webapp.notification

import kr.hhp227.groupsns_webapp.common.db.DbSessionMapper
import kr.hhp227.groupsns_webapp.common.exception.NotificationNotFoundException
import kr.hhp227.groupsns_webapp.notification.dto.NotificationResponse
import kr.hhp227.groupsns_webapp.notification.dto.UnreadCountResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationService(
    private val notificationMapper: NotificationMapper,
    private val dbSessionMapper: DbSessionMapper
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
    @Transactional
    fun notify(recipientId: Long, type: NotificationType, targetType: NotificationTargetType?, targetId: Long?) {
        notificationMapper.insert(NewNotificationRecord(recipientId, type, targetType, targetId))
    }

    @Transactional
    fun notifyAll(recipientIds: Collection<Long>, type: NotificationType, targetType: NotificationTargetType?, targetId: Long?) {
        recipientIds.forEach { notify(it, type, targetType, targetId) }
    }
}
