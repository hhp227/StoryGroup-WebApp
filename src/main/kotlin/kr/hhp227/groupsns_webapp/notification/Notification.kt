package kr.hhp227.groupsns_webapp.notification

import java.time.OffsetDateTime

enum class NotificationType {
    NEW_POST, COMMENT, LIKE, MENTION, CHAT, MEETING_STARTED, NOTICE, INVITE
}

enum class NotificationTargetType {
    POST, REPLY, MESSAGE, MEETING, GROUP
}

data class Notification(
    val id: Long,
    val userId: Long,
    val type: NotificationType,
    val targetType: NotificationTargetType?,
    val targetId: Long?,
    val isRead: Boolean,
    val createdAt: OffsetDateTime
)

class NewNotificationRecord(
    val userId: Long,
    val type: NotificationType,
    val targetType: NotificationTargetType?,
    val targetId: Long?
)
