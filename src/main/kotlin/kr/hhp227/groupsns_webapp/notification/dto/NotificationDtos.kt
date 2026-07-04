package kr.hhp227.groupsns_webapp.notification.dto

import kr.hhp227.groupsns_webapp.notification.Notification
import kr.hhp227.groupsns_webapp.notification.NotificationTargetType
import kr.hhp227.groupsns_webapp.notification.NotificationType
import java.time.OffsetDateTime

data class NotificationResponse(
    val id: Long,
    val type: NotificationType,
    val targetType: NotificationTargetType?,
    val targetId: Long?,
    val isRead: Boolean,
    val createdAt: OffsetDateTime
) {
    companion object {
        fun from(notification: Notification) = NotificationResponse(
            id = notification.id,
            type = notification.type,
            targetType = notification.targetType,
            targetId = notification.targetId,
            isRead = notification.isRead,
            createdAt = notification.createdAt
        )
    }
}

data class UnreadCountResponse(val count: Long)
