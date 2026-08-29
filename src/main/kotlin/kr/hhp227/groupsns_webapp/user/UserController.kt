package kr.hhp227.groupsns_webapp.user

import kr.hhp227.groupsns_webapp.security.UserPrincipal
import kr.hhp227.groupsns_webapp.user.dto.ChangePasswordRequest
import kr.hhp227.groupsns_webapp.user.dto.DeleteAccountRequest
import kr.hhp227.groupsns_webapp.user.dto.ProfileResponse
import kr.hhp227.groupsns_webapp.user.dto.PushPreferencesResponse
import kr.hhp227.groupsns_webapp.user.dto.UpdateProfileRequest
import kr.hhp227.groupsns_webapp.user.dto.UpdatePushPreferencesRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@RequestMapping("/api/users/me")
class UserController(private val userService: UserService) {

    @GetMapping
    fun getProfile(@AuthenticationPrincipal principal: UserPrincipal): ProfileResponse =
        userService.getProfile(principal.id)

    @PatchMapping
    fun updateProfile(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: UpdateProfileRequest
    ): ProfileResponse = userService.updateProfile(principal.id, request)

    @PatchMapping("/password")
    fun changePassword(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<Void> {
        userService.changePassword(principal.id, request)
        return ResponseEntity.noContent().build()
    }

    @DeleteMapping
    fun deleteAccount(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: DeleteAccountRequest
    ): ResponseEntity<Void> {
        userService.deleteAccount(principal.id, request)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/push-preferences")
    fun getPushPreferences(@AuthenticationPrincipal principal: UserPrincipal): PushPreferencesResponse =
        userService.getPushPreferences(principal.id)

    // 전체 교체(두 플래그 모두 필수) — 토글 하나를 바꿔도 클라가 현재 값 둘 다 보낸다
    @PutMapping("/push-preferences")
    fun updatePushPreferences(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: UpdatePushPreferencesRequest
    ): ResponseEntity<Void> {
        userService.updatePushPreferences(principal.id, request)
        return ResponseEntity.noContent().build()
    }
}
