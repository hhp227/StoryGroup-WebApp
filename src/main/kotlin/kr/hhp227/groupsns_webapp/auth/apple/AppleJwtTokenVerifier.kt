package kr.hhp227.groupsns_webapp.auth.apple

import kr.hhp227.groupsns_webapp.common.exception.InvalidAppleTokenException
import org.springframework.stereotype.Component

@Component
class AppleJwtTokenVerifier(
    private val properties: AppleOAuthProperties,
    keySource: AppleJwksKeySource
) : AppleTokenVerifier {
    private val parser = AppleIdTokenParser(properties.clientIds, keySource::find)

    override fun verify(idToken: String): AppleIdentity {
        // 폐기(5.1.1(v))를 못 하는 상태로 애플 계정을 만들지 않는다 — 키가 없으면 로그인 자체를 막는다
        if (!properties.isConfigured) throw InvalidAppleTokenException(InvalidAppleTokenException.NOT_CONFIGURED)
        return parser.parse(idToken)
    }
}
