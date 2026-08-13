package kr.hhp227.groupsns_webapp.realtime

// 개인 큐(/user/queue/notifications)로 나가는 친구 프레즌스 전환 이벤트 —
// "이 유저를 친구로 등록한 사람들"에게만 발행된다(단방향 친구의 역방향 팬아웃).
// 전환 순간의 증분만 싣는다: 전체 상태 복구는 친구 목록 응답의 online 스냅샷이 담당.
data class PresenceSocketEvent(
    val userId: Long,
    val online: Boolean
) {
    val type: String = "PRESENCE_CHANGED"
}
