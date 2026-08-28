package kr.hhp227.groupsns_webapp.push

import kr.hhp227.groupsns_webapp.notification.NotificationType
import kr.hhp227.groupsns_webapp.realtime.ChatBadgeSocketEvent
import kr.hhp227.groupsns_webapp.realtime.NotificationSocketEvent

// 푸시 title/body/data 조합 — 문구는 웹 notification-list.tsx의 타입 라벨과 미러(설계 §4).
// 표시는 이 문자열을 전 플랫폼이 그대로 쓴다(클라 인앱 렌더 문자열과 의미상 중복 — 의도됨).
object PushMessageFactory {

    private val TYPE_LABELS = mapOf(
        NotificationType.NEW_POST to "새 게시글",
        NotificationType.COMMENT to "댓글",
        NotificationType.LIKE to "좋아요",
        NotificationType.MENTION to "멘션",
        NotificationType.CHAT to "채팅 메시지",
        NotificationType.MEETING_STARTED to "화상회의 시작",
        NotificationType.NOTICE to "공지",
        NotificationType.INVITE to "초대",
        NotificationType.JOIN_REQUEST to "가입 신청",
        NotificationType.JOIN_APPROVED to "가입 승인",
        NotificationType.JOIN_REJECTED to "가입 거절"
    )

    fun from(event: NotificationSocketEvent): PushContent {
        val n = event.notification
        val label = TYPE_LABELS[n.type] ?: n.type.name
        val title = n.groupName?.let { "$it · $label" } ?: label
        val data = buildMap {
            put("kind", "NOTIFICATION")
            put("notificationId", n.id.toString())
            put("type", n.type.name)
            n.postId?.let { put("postId", it.toString()) }
            n.groupId?.let { put("groupId", it.toString()) }
        }
        return PushContent(title, n.postPreview, data)
    }

    fun from(event: ChatBadgeSocketEvent): PushContent {
        val bodyText = event.text?.takeIf { it.isNotBlank() } ?: attachmentLabel(event.attachmentType)
        val title = event.roomName ?: event.senderName
        val body = if (event.roomName != null) "${event.senderName}: $bodyText" else bodyText
        val data = buildMap {
            put("kind", "CHAT")
            put("chatRoomId", event.chatRoomId.toString())
            put("roomTitle", title)
            event.groupId?.let { put("groupId", it.toString()) }
        }
        return PushContent(title, body, data)
    }

    private fun attachmentLabel(contentType: String?): String = when {
        contentType == null -> ""
        contentType.startsWith("image") -> "사진"
        contentType.startsWith("video") -> "동영상"
        else -> "파일"
    }
}
