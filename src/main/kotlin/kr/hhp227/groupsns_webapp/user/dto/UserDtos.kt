package kr.hhp227.groupsns_webapp.user.dto

import kr.hhp227.groupsns_webapp.user.User
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

data class ProfileResponse(
    val id: Long,
    val name: String,
    val email: String,
    val profileImg: String?,
    val bio: String?,
    val statusMessage: String?
) {
    companion object {
        fun from(user: User) = ProfileResponse(
            id = user.id,
            name = user.name,
            email = user.email,
            profileImg = user.profileImg,
            bio = user.bio,
            statusMessage = user.statusMessage
        )
    }
}

data class UpdateProfileRequest(
    @field:NotBlank @field:Size(max = 50) val name: String,
    @field:Size(max = 255) val profileImg: String?,
    @field:Size(max = 500) val bio: String?,
    @field:Size(max = 100) val statusMessage: String?
)
