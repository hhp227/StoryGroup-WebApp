package kr.hhp227.groupsns_webapp.auth.google

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class GoogleOAuthProperties(
    // aud 허용 목록(웹·iOS·Desktop). Android는 웹 클라이언트 ID를 serverClientId로 써서 aud=웹
    @Value("\${app.oauth.google.client-ids}") clientIds: String,
    @Value("\${app.oauth.google.desktop-client-id}") val desktopClientId: String,
    @Value("\${app.oauth.google.desktop-client-secret}") val desktopClientSecret: String
) {
    val clientIds: List<String> = clientIds.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
