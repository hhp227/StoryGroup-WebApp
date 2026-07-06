package kr.hhp227.groupsns_webapp.realtime

import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

// RTC 시그널링의 브로드캐스트 출구 — ChatEventBroadcaster/NotificationBroadcaster와 같은 역할.
// 전부 휘발성(트랜잭션 없음)이라 리스너 없이 직접 호출한다. 다중 인스턴스로 확장할 때
// 교체 지점이 되는 것도 동일(설계 문서 D2@Phase6).
@Component
class RtcBroadcaster(private val messagingTemplate: SimpMessagingTemplate) {

    fun relayPeers(roomKey: String, peers: List<RtcPeer>) {
        messagingTemplate.convertAndSend("/topic/rtc/$roomKey", RtcPeersEvent(roomKey, peers))
    }

    // 사용자 라우팅 키는 세션 Principal.name(userId 문자열, StompUserToken) — 알림 큐와 같은 방식.
    fun relaySignal(toUserId: Long, event: RtcSignalEvent) {
        messagingTemplate.convertAndSendToUser(toUserId.toString(), "/queue/rtc", event)
    }

    // 벨울림은 통화 화면이 아니라 어느 화면에서든 받아야 하므로 전역 구독인 알림 큐로 보낸다(D6).
    fun relayInvite(toUserId: Long, event: CallInviteEvent) {
        messagingTemplate.convertAndSendToUser(toUserId.toString(), "/queue/notifications", event)
    }
}
