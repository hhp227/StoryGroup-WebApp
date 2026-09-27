package kr.hhp227.groupsns_webapp.auth

import kr.hhp227.groupsns_webapp.auth.apple.AppleCallbackState
import kr.hhp227.groupsns_webapp.auth.dto.AppleExchangeRequest
import kr.hhp227.groupsns_webapp.auth.dto.AppleLoginRequest
import kr.hhp227.groupsns_webapp.auth.dto.GoogleAccessTokenLoginRequest
import kr.hhp227.groupsns_webapp.auth.dto.GoogleCodeLoginRequest
import kr.hhp227.groupsns_webapp.auth.dto.GoogleLoginRequest
import kr.hhp227.groupsns_webapp.auth.dto.LoginRequest
import kr.hhp227.groupsns_webapp.auth.dto.RefreshTokenRequest
import kr.hhp227.groupsns_webapp.auth.dto.RegisterRequest
import kr.hhp227.groupsns_webapp.auth.dto.TokenResponse
import kr.hhp227.groupsns_webapp.auth.dto.UserSummaryResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.util.HtmlUtils
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

    @PostMapping("/google")
    fun loginWithGoogle(@Valid @RequestBody request: GoogleLoginRequest, httpRequest: HttpServletRequest): TokenResponse =
        authService.loginWithGoogleIdToken(request.idToken, clientIp(httpRequest), httpRequest.getHeader("User-Agent"))

    @PostMapping("/google/access-token")
    fun loginWithGoogleAccessToken(
        @Valid @RequestBody request: GoogleAccessTokenLoginRequest,
        httpRequest: HttpServletRequest
    ): TokenResponse =
        authService.loginWithGoogleAccessToken(request.accessToken, clientIp(httpRequest), httpRequest.getHeader("User-Agent"))

    @PostMapping("/google/code")
    fun loginWithGoogleCode(@Valid @RequestBody request: GoogleCodeLoginRequest, httpRequest: HttpServletRequest): TokenResponse =
        authService.loginWithGoogleCode(
            request.code, request.codeVerifier, request.redirectUri,
            clientIp(httpRequest), httpRequest.getHeader("User-Agent")
        )

    @PostMapping("/apple")
    fun loginWithApple(@Valid @RequestBody request: AppleLoginRequest, httpRequest: HttpServletRequest): TokenResponse =
        authService.loginWithApple(request, clientIp(httpRequest), httpRequest.getHeader("User-Agent"))

    // 애플 form_post(response_mode=form_post) — Android·Desktop 전용. 결과는 앱 복귀 페이지
    @PostMapping("/apple/callback", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun appleCallback(
        @RequestParam(required = false) state: String?,
        @RequestParam(name = "id_token", required = false) idToken: String?,
        @RequestParam(required = false) code: String?,
        @RequestParam(required = false) user: String?,
        @RequestParam(required = false) error: String?
    ): ResponseEntity<String> {
        val html = MediaType("text", "html", Charsets.UTF_8)
        val parsed = state?.let(AppleCallbackState::parse)
            ?: return ResponseEntity.badRequest().contentType(html).body(BAD_STATE_HTML)
        val url = authService.completeAppleCallback(parsed, idToken, code, user, error)
        return ResponseEntity.ok().contentType(html).body(appleReturnHtml(url))
    }

    @PostMapping("/apple/exchange")
    fun exchangeAppleCode(@Valid @RequestBody request: AppleExchangeRequest, httpRequest: HttpServletRequest): TokenResponse =
        authService.exchangeAppleCode(request.code, request.verifier, clientIp(httpRequest), httpRequest.getHeader("User-Agent"))

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

    companion object {
        private const val BAD_STATE_HTML = "<!doctype html><meta charset=utf-8><title>StoryGroup</title>" +
            "<p style=\"font-family:sans-serif;padding:2em\">로그인 요청이 올바르지 않습니다. 앱에서 다시 시도해주세요.</p>"

        // 302 대신 페이지 — 폼 POST 응답의 커스텀 스킴 리다이렉트를 막는 브라우저가 있어 버튼을 폴백으로 둔다
        internal fun appleReturnHtml(url: String): String {
            val href = HtmlUtils.htmlEscape(url)
            return "<!doctype html><meta charset=utf-8><meta name=viewport content=\"width=device-width,initial-scale=1\">" +
                "<title>StoryGroup</title>" +
                "<p style=\"font-family:sans-serif;padding:2em\">로그인 처리가 끝났습니다.<br><br>" +
                "<a id=go href=\"$href\" style=\"display:inline-block;padding:12px 20px;background:#000;color:#fff;border-radius:8px;text-decoration:none\">앱으로 돌아가기</a></p>" +
                "<script>location.replace(document.getElementById('go').href)</script>"
        }
    }
}
