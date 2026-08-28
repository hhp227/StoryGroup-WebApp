package kr.hhp227.groupsns_webapp.push.dto

import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size

enum class PushPlatform { ANDROID, IOS, WEB }

data class RegisterPushTokenRequest(
    @field:NotBlank @field:Size(max = 512) val token: String,
    val platform: PushPlatform
)
