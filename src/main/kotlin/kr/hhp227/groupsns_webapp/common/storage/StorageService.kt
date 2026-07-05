package kr.hhp227.groupsns_webapp.common.storage

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

// Supabase Storage 업로드. 클라이언트가 직접 올리지 않고 서버를 경유하는 이유:
// 이 앱은 Supabase Auth가 아니라 자체 JWT를 쓰므로 Storage RLS로 사용자를 식별할 수 없고,
// anon 키를 프론트에 노출하면 아무나 업로드할 수 있게 된다. service_role 키는 서버에만 둔다.
@Service
class StorageService(
    @Value("\${app.storage.url}") private val storageUrl: String,
    @Value("\${app.storage.service-key}") private val serviceKey: String,
    @Value("\${app.storage.bucket}") private val bucket: String
) {
    private val restTemplate = RestTemplate()

    // 업로드 후 공개 URL을 반환한다. 버킷은 public으로 두되 경로에 UUID가 들어가 추측이 어렵다.
    fun upload(path: String, bytes: ByteArray, contentType: String?): String {
        if (serviceKey.isBlank()) {
            throw IllegalArgumentException("파일 저장소가 아직 설정되지 않았습니다")
        }
        val headers = HttpHeaders().apply {
            set("apikey", serviceKey)
            setBearerAuth(serviceKey)
            this.contentType = parseContentTypeOrDefault(contentType)
        }
        restTemplate.exchange(
            "$storageUrl/storage/v1/object/$bucket/$path",
            HttpMethod.POST,
            HttpEntity(bytes, headers),
            String::class.java
        )
        return "$storageUrl/storage/v1/object/public/$bucket/$path"
    }

    private fun parseContentTypeOrDefault(contentType: String?): MediaType =
        try {
            MediaType.parseMediaType(contentType ?: "")
        } catch (e: Exception) {
            MediaType.APPLICATION_OCTET_STREAM
        }
}
