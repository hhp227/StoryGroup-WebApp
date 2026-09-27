package kr.hhp227.groupsns_webapp.auth

// provider(구글·애플) 공통 신원 — 계정 결정(resolveOauthUser)의 입력.
// email은 애플 재로그인에서 빠질 수 있어 nullable(기존 연결이 없으면 신규 생성 불가 → 401)
data class OauthIdentity(
    val sub: String,
    val email: String?,
    val emailVerified: Boolean,
    val name: String?,
    val picture: String?
)
