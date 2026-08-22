package kr.hhp227.groupsns_webapp.notification.dto

import kr.hhp227.groupsns_webapp.notification.Notification
import kr.hhp227.groupsns_webapp.notification.NotificationTargetType
import kr.hhp227.groupsns_webapp.notification.NotificationType
import java.time.OffsetDateTime

// postId/postPreview(본문 앞 60자)/groupId/groupName은 조회 시점 역추적 컨텍스트 —
// 대상이 삭제됐거나 접근 불가면 null(클라이언트는 타입 라벨만 표시로 강등)
data class NotificationResponse(
    val id: Long,
    val type: NotificationType,
    val targetType: NotificationTargetType?,
    val targetId: Long?,
    val isRead: Boolean,
    val createdAt: OffsetDateTime,
    val postId: Long?,
    val postPreview: String?,
    val groupId: Long?,
    val groupName: String?
) {
    companion object {
        fun from(notification: Notification) = NotificationResponse(
            id = notification.id,
            type = notification.type,
            targetType = notification.targetType,
            targetId = notification.targetId,
            isRead = notification.isRead,
            createdAt = notification.createdAt,
            postId = notification.postId,
            postPreview = notification.postPreview,
            groupId = notification.groupId,
            groupName = notification.groupName
        )
    }
}

data class UnreadCountResponse(val count: Long)
