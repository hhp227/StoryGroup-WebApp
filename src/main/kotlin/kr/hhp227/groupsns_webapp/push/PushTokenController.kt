package kr.hhp227.groupsns_webapp.push

import kr.hhp227.groupsns_webapp.push.dto.RegisterPushTokenRequest
import kr.hhp227.groupsns_webapp.security.UserPrincipal
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@RequestMapping("/api/push-tokens")
class PushTokenController(private val pushTokenService: PushTokenService) {

    // 멱등 — 로그인 직후·FCM 토큰 갱신 때마다 클라가 호출한다
    @PutMapping
    fun register(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: RegisterPushTokenRequest
    ): ResponseEntity<Void> {
        pushTokenService.register(principal.id, request)
        return ResponseEntity.ok().build()
    }

    @DeleteMapping
    fun unregister(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam token: String
    ): ResponseEntity<Void> {
        pushTokenService.unregister(token)
        return ResponseEntity.noContent().build()
    }
}
