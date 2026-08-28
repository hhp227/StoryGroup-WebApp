package kr.hhp227.groupsns_webapp.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import kr.hhp227.groupsns_webapp.push.FcmSender
import kr.hhp227.groupsns_webapp.push.FirebaseFcmSender
import kr.hhp227.groupsns_webapp.push.NoopFcmSender
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.IOException

// Cloud Run은 기본 서비스 계정 ADC로 인증된다(같은 GCP 프로젝트에 Firebase 추가, 설계 §3).
// 로컬 실행에서 ADC가 없으면 발송만 조용히 비활성 — 서버의 나머지는 Firebase 없이 돌아가야 한다.
@Configuration
class FirebaseConfig {
    private val log = LoggerFactory.getLogger(FirebaseConfig::class.java)

    @Bean
    fun fcmSender(): FcmSender = try {
        val app = FirebaseApp.getApps().firstOrNull() ?: FirebaseApp.initializeApp(
            FirebaseOptions.builder().setCredentials(GoogleCredentials.getApplicationDefault()).build()
        )
        FirebaseFcmSender(FirebaseMessaging.getInstance(app))
    } catch (e: IOException) {
        log.warn("ADC 없음 — 푸시 발송 비활성(로컬 실행): {}", e.message)
        NoopFcmSender()
    }
}
