package kr.hhp227.groupsns_webapp.push

// 서버가 조합한 푸시 1건 — data 키 사전은 설계 §4(전 플랫폼 공유, 값은 전부 문자열)
data class PushContent(
    val title: String,
    val body: String?,
    val data: Map<String, String>
)

// FCM 호출 경계 — 테스트는 페이크로 대체하고, ADC 없는 로컬 실행은 Noop이 담당한다
interface FcmSender {
    /** 발송 후 삭제해야 할 무효 토큰(UNREGISTERED·INVALID_ARGUMENT)을 돌려준다 */
    fun send(tokens: List<String>, content: PushContent): List<String>
}

class NoopFcmSender : FcmSender {
    override fun send(tokens: List<String>, content: PushContent): List<String> = emptyList()
}
