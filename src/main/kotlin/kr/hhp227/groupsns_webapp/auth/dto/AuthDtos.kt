package kr.hhp227.groupsns_webapp.auth.dto

import kr.hhp227.groupsns_webapp.user.User
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank
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
