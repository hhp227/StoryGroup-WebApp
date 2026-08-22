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

// post_*/group_*는 target 참조를 조회 시점에 역추적한 파생 컨텍스트(컬럼 아님) —
// POST=직접, REPLY=user_replys 경유, GROUP=그룹 직결. 삭제/탈퇴로 못 푸는 참조는 null로 강등된다.
data class Notification(
    val id: Long,
    val userId: Long,
    val type: NotificationType,
    val targetType: NotificationTargetType?,
    val targetId: Long?,
    val isRead: Boolean,
    val createdAt: OffsetDateTime,
    val postId: Long?,
    val postPreview: String?,
    val groupId: Long?,
    val groupName: String?
)

class NewNotificationRecord(
    val userId: Long,
    val type: NotificationType,
    val targetType: NotificationTargetType?,
    val targetId: Long?
) {
    var id: Long = 0 // useGeneratedKeys로 채워짐 — 생성 직후 WS 페이로드 조회에 필요
}
