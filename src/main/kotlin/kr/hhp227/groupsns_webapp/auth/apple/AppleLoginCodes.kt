package kr.hhp227.groupsns_webapp.auth.apple

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class AppleLoginCodes(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${app.oauth.apple.login-code-ttl-ms}") private val ttlMillis: Long
)
