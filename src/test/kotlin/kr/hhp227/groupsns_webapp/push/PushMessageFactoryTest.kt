package kr.hhp227.groupsns_webapp.push

import kr.hhp227.groupsns_webapp.notification.NotificationTargetType
import kr.hhp227.groupsns_webapp.notification.NotificationType
import kr.hhp227.groupsns_webapp.notification.dto.NotificationResponse
import kr.hhp227.groupsns_webapp.realtime.ChatBadgeSocketEvent
import kr.hhp227.groupsns_webapp.realtime.NotificationSocketEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class PushMessageFactoryTest {
    // NotificationResponse의 실제 생성자(notification/dto/NotificationDtos.kt)와 필드 순서 일치 확인됨
    private fun notificationEvent(
        type: NotificationType,
        groupName: String? = "우리모임",
        postPreview: String? = "미리보기",
        postId: Long? = 7,
        groupId: Long? = 3
    ) = NotificationSocketEvent(
        recipientId = 1,
        notification = NotificationResponse(
            id = 42, type = type, targetType = NotificationTargetType.POST, targetId = 7,
            isRead = false, createdAt = OffsetDateTime.now(),
            postId = postId, postPreview = postPreview, groupId = groupId, groupName = groupName
        )
    )

    @Test
    fun `알림 - 그룹명과 미리보기가 title·body로 간다`() {
        val content = PushMessageFactory.from(notificationEvent(NotificationType.COMMENT))
        assertEquals("우리모임 · 댓글", content.title)
        assertEquals("미리보기", content.body)
        assertEquals("NOTIFICATION", content.data["kind"])
        assertEquals("42", content.data["notificationId"])
        assertEquals("COMMENT", content.data["type"])
        assertEquals("7", content.data["postId"])
        assertEquals("3", content.data["groupId"])
    }

    @Test
    fun `알림 - 그룹명 없으면 라벨만, 미리보기 없으면 body 없음`() {
        val content = PushMessageFactory.from(
            notificationEvent(NotificationType.JOIN_APPROVED, groupName = null, postPreview = null, postId = null)
        )
        assertEquals("가입 승인", content.title)
        assertNull(content.body)
        assertEquals(false, content.data.containsKey("postId"))
    }

    private fun chatEvent(
        roomName: String? = "우리모임",
        groupId: Long? = 3,
        text: String = "안녕",
        attachmentType: String? = null
    ) = ChatBadgeSocketEvent(
        recipientId = 1, chatRoomId = 5, messageId = 10, senderId = 2,
        senderName = "홍길동", roomName = roomName, groupId = groupId,
        text = text, attachmentType = attachmentType, createdAt = OffsetDateTime.now()
    )

    @Test
    fun `채팅 그룹방 - title은 방이름, body는 발신자 접두`() {
        val content = PushMessageFactory.from(chatEvent())
        assertEquals("우리모임", content.title)
        assertEquals("홍길동: 안녕", content.body)
        assertEquals("CHAT", content.data["kind"])
        assertEquals("5", content.data["chatRoomId"])
        assertEquals("3", content.data["groupId"])
        assertEquals("우리모임", content.data["roomTitle"])
    }

    @Test
    fun `채팅 DM - title은 발신자, 첨부 전용은 종류 라벨`() {
        val content = PushMessageFactory.from(chatEvent(roomName = null, groupId = null, text = "", attachmentType = "image/jpeg"))
        assertEquals("홍길동", content.title)
        assertEquals("사진", content.body)
        assertEquals("홍길동", content.data["roomTitle"])
        assertEquals(false, content.data.containsKey("groupId"))
    }
}
