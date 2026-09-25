package kr.hhp227.groupsns_webapp.auth.dto

import kr.hhp227.groupsns_webapp.user.User
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Pattern
import javax.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank @field:Size(max = 50) val name: String,
    @field:NotBlank @field:Email @field:Size(max = 255) val email: String,
    // bcrypt는 72바이트를 넘는 입력을 자르므로 상한을 둠
    @field:NotBlank @field:Size(min = 8, max = 72) val password: String
)

data class LoginRequest(
    @field:NotBlank @field:Email val email: String,
    @field:NotBlank val password: String
)

data class GoogleLoginRequest(
    @field:NotBlank val idToken: String
)

// 웹 커스텀 버튼(GIS 토큰 클라이언트 팝업)이 받은 액세스 토큰
data class GoogleAccessTokenLoginRequest(
    @field:NotBlank val accessToken: String
)

// Desktop 루프백 PKCE(설계 §2.1) — redirectUri는 인가 요청 때와 같아야 구글이 교환해 준다
data class GoogleCodeLoginRequest(
    @field:NotBlank val code: String,
    @field:NotBlank val codeVerifier: String,
    @field:NotBlank @field:Pattern(regexp = "^http://127\\.0\\.0\\.1:\\d{1,5}(/.*)?$") val redirectUri: String
)

data class RefreshTokenRequest(
    @field:NotBlank val refreshToken: String
)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long
)

data class UserSummaryResponse(
    val id: Long,
    val name: String,
    val email: String,
    val profileImg: String?
) {
    companion object {
        fun from(user: User) = UserSummaryResponse(user.id, user.name, user.email, user.profileImg)
    }
}
