package kr.hhp227.groupsns_webapp.auth.apple

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class AppleOAuthProperties(
    // aud 허용 목록: iOS 번들 ID(네이티브), Services ID(웹·Android·Desktop)
    @Value("\${app.oauth.apple.client-ids}") clientIds: String,
    @Value("\${app.oauth.apple.services-id}") val servicesId: String,
    @Value("\${app.oauth.apple.team-id}") val teamId: String,
    @Value("\${app.oauth.apple.key-id}") val keyId: String,
    @Value("\${app.oauth.apple.private-key}") val privateKey: String,
    // 인가 때 쓴 redirect_uri와 같아야 애플이 코드를 교환해 준다
    @Value("\${app.oauth.apple.web-redirect-uri}") val webRedirectUri: String,
    @Value("\${app.oauth.apple.callback-url}") val callbackUrl: String
) {
    val clientIds: List<String> = clientIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }

    // client_secret 서명 재료가 다 있어야 교환·폐기가 가능 — 없으면 세 엔드포인트 모두 401(부팅은 정상)
    val isConfigured: Boolean get() = teamId.isNotEmpty() && keyId.isNotEmpty() && privateKey.isNotEmpty()
}
