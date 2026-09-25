package kr.hhp227.groupsns_webapp.auth.google

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import kr.hhp227.groupsns_webapp.common.exception.InvalidGoogleTokenException
import org.springframework.stereotype.Component

// 구글 공개키(JWKS)는 라이브러리가 캐시한다. verify()는 서명·iss·exp·aud를 모두 검사하고 실패 시 null
@Component
class GoogleApiTokenVerifier(properties: GoogleOAuthProperties) : GoogleTokenVerifier {
    private val verifier = GoogleIdTokenVerifier.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance())
        .setAudience(properties.clientIds)
        .build()

    override fun verify(idToken: String): GoogleIdentity {
        val token = try {
            verifier.verify(idToken)
        } catch (e: Exception) {
            // 형식이 깨진 토큰은 IllegalArgumentException, 키 조회 실패는 IOException 등 — 전부 인증 실패로 본다
            null
        } ?: throw InvalidGoogleTokenException()
        val payload = token.payload
        val email = payload.email ?: throw InvalidGoogleTokenException()
        return GoogleIdentity(
            sub = payload.subject,
            email = email,
            emailVerified = payload.emailVerified == true,
            name = payload["name"] as? String,
            picture = payload["picture"] as? String
        )
    }
}
