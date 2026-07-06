package kr.hhp227.groupsns_webapp.common.storage

import kr.hhp227.groupsns_webapp.security.UserPrincipal
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

data class UploadedFileResponse(
    val url: String,
    val name: String,
    val contentType: String?,
    val size: Long
)

// /api/images의 일반 파일판 — MIME 제한 없이 아무 파일이나 올리고 공개 URL+메타데이터를 돌려준다.
// 그룹 파일 공유(/api/groups/{id}/files/upload)와 달리 그룹 스코프/DB 행 없이 스토리지에만 저장 —
// 채팅 첨부처럼 "URL 문자열을 받는 API"에 넣을 파일용. 크기 상한은 서버 전역 multipart 설정(20MB)에 맡긴다.
@RestController
@RequestMapping("/api/files")
class FileUploadController(private val storageService: StorageService) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<UploadedFileResponse> {
        if (file.isEmpty) throw IllegalArgumentException("빈 파일은 업로드할 수 없습니다")

        // FileService.uploadBinaryFile과 같은 규칙 — 저장 이름은 UUID(+확장자)로 특수문자/한글 문제 회피.
        val originalName = file.originalFilename?.takeIf { it.isNotBlank() } ?: "파일"
        val extension = originalName.substringAfterLast('.', "").filter { it.isLetterOrDigit() }.take(10)
        val path = "files/user-${principal.id}/${UUID.randomUUID()}" +
            (if (extension.isNotEmpty()) ".$extension" else "")
        val url = storageService.upload(path, file.bytes, file.contentType)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(UploadedFileResponse(url, originalName.take(255), file.contentType?.take(100), file.size))
    }
}
