package kr.hhp227.groupsns_webapp.auth.apple

// 서명·iss·exp·aud 검증을 통과한 애플 id_token의 필요한 클레임. audience는 교환·폐기의 client_id로 재사용
data class AppleIdentity(
    val sub: String,
    val email: String?,
    val emailVerified: Boolean,
    val isPrivateEmail: Boolean,
    val audience: String
)

// 테스트에서 mock으로 바꾸기 위한 경계 — 실패는 InvalidAppleTokenException
interface AppleTokenVerifier {
    fun verify(idToken: String): AppleIdentity
}

// authorization code → Apple refresh_token. 실패·미설정은 null(로그인은 막지 않는다 — 설계 §2.2)
interface AppleCodeExchanger {
    fun exchange(code: String, clientId: String, redirectUri: String?): String?
}

// 탈퇴 시 폐기(5.1.1(v)) — 실패는 예외, 호출부가 로그만 남긴다
interface AppleTokenRevoker {
    fun revoke(refreshToken: String, clientId: String)
}
