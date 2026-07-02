package kr.hhp227.groupsns_webapp.auth

import kr.hhp227.groupsns_webapp.auth.dto.LoginRequest
import kr.hhp227.groupsns_webapp.auth.dto.RefreshTokenRequest
import kr.hhp227.groupsns_webapp.auth.dto.RegisterRequest
import kr.hhp227.groupsns_webapp.auth.dto.TokenResponse
import kr.hhp227.groupsns_webapp.auth.dto.UserSummaryResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<UserSummaryResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request))

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest, httpRequest: HttpServletRequest): TokenResponse =
        authService.login(request, clientIp(httpRequest), httpRequest.getHeader("User-Agent"))

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest, httpRequest: HttpServletRequest): TokenResponse =
        authService.refresh(request, httpRequest.getHeader("User-Agent"))

    @PostMapping("/logout")
    fun logout(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<Void> {
        authService.logout(request)
        return ResponseEntity.noContent().build()
    }

    // Cloud Run은 로드밸런서 뒤에서 실행되므로 원 클라이언트 IP는 X-Forwarded-For의 첫 값을 사용
    private fun clientIp(request: HttpServletRequest): String {
        val forwardedFor = request.getHeader("X-Forwarded-For")
        return forwardedFor?.split(",")?.firstOrNull()?.trim() ?: request.remoteAddr
    }
}
