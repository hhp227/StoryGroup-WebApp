package kr.hhp227.groupsns_webapp.user

import kr.hhp227.groupsns_webapp.security.UserPrincipal
import kr.hhp227.groupsns_webapp.user.dto.ProfileResponse
import kr.hhp227.groupsns_webapp.user.dto.UpdateProfileRequest
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
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
}
