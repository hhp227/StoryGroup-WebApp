package kr.hhp227.groupsns_webapp.push

import com.google.firebase.messaging.AndroidConfig
import com.google.firebase.messaging.ApnsConfig
import com.google.firebase.messaging.Aps
import com.google.firebase.messaging.ApsAlert
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage

// MulticastMessage 하나에 플랫폼별 config 동시 지정(설계 §4):
// top-level notification 없이 — Android·웹은 data-only(클라 렌더=포그라운드 억제 제어권),
// iOS만 ApnsConfig alert(앱 종료 상태 포함 OS 표시).
class FirebaseFcmSender(private val messaging: FirebaseMessaging) : FcmSender {

    override fun send(tokens: List<String>, content: PushContent): List<String> {
        val data = buildMap {
            putAll(content.data)
            put("title", content.title)
            content.body?.let { put("body", it) }
        }
        val alert = ApsAlert.builder().setTitle(content.title)
            .apply { content.body?.let { setBody(it) } }
            .build()
        val message = MulticastMessage.builder()
            .addAllTokens(tokens)
            .putAllData(data)
            .setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build())
            .setApnsConfig(ApnsConfig.builder().setAps(Aps.builder().setAlert(alert).setSound("default").build()).build())
            .build()
        val response = messaging.sendEachForMulticast(message)
        return response.responses.mapIndexedNotNull { i, r ->
            when (r.exception?.messagingErrorCode) {
                MessagingErrorCode.UNREGISTERED, MessagingErrorCode.INVALID_ARGUMENT -> tokens[i]
                else -> null
            }
        }
    }
}
