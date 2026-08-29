package kr.hhp227.groupsns_webapp.user.dto

import kr.hhp227.groupsns_webapp.user.PushPreferences
import kr.hhp227.groupsns_webapp.user.User
import kr.hhp227.groupsns_webapp.user.UserRole
import java.time.OffsetDateTime
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

// 다른 사용자가 보는 공개 프로필 — ProfileResponse(본인용)와 달리 이메일은 내려주지 않는다.
data class PublicProfileResponse(
    val id: Long,
    val name: String,
    val profileImg: String?,
    val bio: String?,
    val statusMessage: String?,
    val createdAt: OffsetDateTime
) {
    companion object {
        fun from(user: User) = PublicProfileResponse(
            id = user.id,
            name = user.name,
            profileImg = user.profileImg,
            bio = user.bio,
            statusMessage = user.statusMessage,
            createdAt = user.createdAt
        )
    }
}

data class ProfileResponse(
    val id: Long,
    val name: String,
    val email: String,
    val profileImg: String?,
    val bio: String?,
    val statusMessage: String?,
    // 웹이 운영자 메뉴(사용자 신고 관리) 노출 여부를 결정하는 데 쓴다. 실제 인가는 서버가 다시 검사한다.
    val isAdmin: Boolean
) {
    companion object {
        fun from(user: User) = ProfileResponse(
            id = user.id,
            name = user.name,
            email = user.email,
            profileImg = user.profileImg,
            bio = user.bio,
            statusMessage = user.statusMessage,
            isAdmin = user.role == UserRole.ADMIN
        )
    }
}

data class UpdateProfileRequest(
    @field:NotBlank @field:Size(max = 50) val name: String,
    @field:Size(max = 255) val profileImg: String?,
    @field:Size(max = 500) val bio: String?,
    @field:Size(max = 100) val statusMessage: String?
)

data class ChangePasswordRequest(
    @field:NotBlank val currentPassword: String,
    // bcrypt는 72바이트를 넘는 입력을 자르므로 상한을 둠 (RegisterRequest와 동일)
    @field:NotBlank @field:Size(min = 8, max = 72) val newPassword: String
)

data class DeleteAccountRequest(
    @field:NotBlank val password: String
)

// 푸시 종류별 on/off(설계 §2) — 필드명은 wire 계약(웹 api.ts·shared UserDtos.kt가 같은 이름을 쓴다)
data class PushPreferencesResponse(
    val chatEnabled: Boolean,
    val activityEnabled: Boolean
) {
    companion object {
        fun from(prefs: PushPreferences) = PushPreferencesResponse(prefs.chatEnabled, prefs.activityEnabled)
    }
}

// 전체 교체 — 토글 하나를 바꿔도 클라가 현재 값 둘 다 보낸다.
// Kotlin 원시 Boolean은 필드 누락 시 Jackson이 false로 조용히 채우므로(FAIL_ON_NULL_FOR_PRIMITIVES 미설정)
// nullable + @NotNull로 받아 누락을 400(VALIDATION_ERROR)으로 거른다 — 한 필드만 보내 다른 종류를 꺼버리는 사고 방지.
data class UpdatePushPreferencesRequest(
    @field:NotNull val chatEnabled: Boolean?,
    @field:NotNull val activityEnabled: Boolean?
)
