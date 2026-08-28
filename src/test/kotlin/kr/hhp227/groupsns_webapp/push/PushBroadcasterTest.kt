package kr.hhp227.groupsns_webapp.push

import kr.hhp227.groupsns_webapp.realtime.ChatBadgeSocketEvent
import kr.hhp227.groupsns_webapp.realtime.PushBroadcaster
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito

class PushBroadcasterTest {
    private class FakeFcmSender(private val invalid: List<String> = emptyList()) : FcmSender {
        var sentTokens: List<String>? = null
        var sentContent: PushContent? = null
        override fun send(tokens: List<String>, content: PushContent): List<String> {
            sentTokens = tokens
            sentContent = content
            return invalid
        }
    }

    private val mapper = Mockito.mock(PushTokenMapper::class.java)

    private fun chatEvent(recipientId: Long) = ChatBadgeSocketEvent(
        recipientId = recipientId, chatRoomId = 5, messageId = 10, senderId = 2,
        senderName = "홍길동", roomName = null, groupId = null, text = "안녕"
    )

    @Test
    fun `토큰이 없으면 발송하지 않는다`() {
        Mockito.`when`(mapper.findTokensByUser(1)).thenReturn(emptyList())
        val sender = FakeFcmSender()
        PushBroadcaster(mapper, sender).on(chatEvent(1))
        assertEquals(null, sender.sentTokens)
    }

    @Test
    fun `수신자의 전 토큰으로 발송하고 무효 토큰은 삭제한다`() {
        Mockito.`when`(mapper.findTokensByUser(1)).thenReturn(listOf("tokA", "tokB"))
        val sender = FakeFcmSender(invalid = listOf("tokB"))
        PushBroadcaster(mapper, sender).on(chatEvent(1))
        assertEquals(listOf("tokA", "tokB"), sender.sentTokens)
        assertEquals("홍길동", sender.sentContent!!.title)
        Mockito.verify(mapper).deleteByToken("tokB")
        Mockito.verify(mapper, Mockito.never()).deleteByToken("tokA")
    }

    @Test
    fun `발송 예외는 삼킨다 - 리스너가 죽으면 안 된다`() {
        Mockito.`when`(mapper.findTokensByUser(1)).thenReturn(listOf("tokA"))
        val throwing = object : FcmSender {
            override fun send(tokens: List<String>, content: PushContent): List<String> = throw RuntimeException("fcm down")
        }
        PushBroadcaster(mapper, throwing).on(chatEvent(1)) // 예외가 전파되면 테스트 실패
    }
}
