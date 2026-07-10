package kr.hhp227.groupsns_webapp.notification

import java.time.OffsetDateTime

enum class NotificationType {
    NEW_POST, COMMENT, LIKE, MENTION, CHAT, MEETING_STARTED, NOTICE, INVITE,
    // 그룹 가입 신청 흐름: 신청 발생(모더레이터에게) / 승인·거절 결과(신청자에게)
    JOIN_REQUEST, JOIN_APPROVED, JOIN_REJECTED
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
) {
    var id: Long = 0 // useGeneratedKeys로 채워짐 — 생성 직후 WS 페이로드 조회에 필요
}
