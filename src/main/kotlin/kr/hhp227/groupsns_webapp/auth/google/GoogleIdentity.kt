package kr.hhp227.groupsns_webapp.auth.google

// 서명·aud·만료 검증을 통과한 구글 ID 토큰의 필요한 클레임만 추린 값
data class GoogleIdentity(
    val sub: String,
    val email: String,
    val emailVerified: Boolean,
    val name: String?,
    val picture: String?
)

// 테스트에서 mock으로 바꾸기 위한 경계 — 실패는 InvalidGoogleTokenException
interface GoogleTokenVerifier {
    fun verify(idToken: String): GoogleIdentity
}

// 웹 커스텀 버튼(GIS 토큰 클라이언트 팝업)이 받은 액세스 토큰 — aud가 우리 클라이언트인지 구글에 확인한다
interface GoogleAccessTokenVerifier {
    fun verify(accessToken: String): GoogleIdentity
}

// Desktop 루프백 PKCE 코드 → id_token. client_secret이 앱에 들어가지 않도록 서버가 교환한다
interface GoogleCodeExchanger {
    fun exchange(code: String, codeVerifier: String, redirectUri: String): String
}
