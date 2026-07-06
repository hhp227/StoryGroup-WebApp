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

data class UploadedImageResponse(val url: String)

// 게시글/프로필/그룹 대표 이미지처럼 "URL 문자열을 받는 기존 API"에 넣을 이미지를 올리는 범용 엔드포인트.
// 그룹 파일 공유(FileController /upload)와 달리 그룹 스코프/메타데이터 없이 공개 URL만 돌려준다 —
// 어느 리소스에 붙는 이미지든 기존 API 계약(images/profileImg/image의 URL 문자열)이 그대로 유지된다.
@RestController
@RequestMapping("/api/images")
class ImageUploadController(private val storageService: StorageService) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<UploadedImageResponse> {
        if (file.isEmpty) throw IllegalArgumentException("빈 파일은 업로드할 수 없습니다")
        val contentType = file.contentType ?: ""
        if (!contentType.startsWith("image/")) throw IllegalArgumentException("이미지 파일만 업로드할 수 있습니다")
        if (file.size > MAX_IMAGE_BYTES) throw IllegalArgumentException("이미지는 10MB 이하만 업로드할 수 있습니다")

        // FileService.uploadBinaryFile과 같은 규칙 — 저장 이름은 UUID(+확장자)로 특수문자/한글 문제 회피.
        val extension = (file.originalFilename ?: "")
            .substringAfterLast('.', "").filter { it.isLetterOrDigit() }.take(10)
        val path = "images/user-${principal.id}/${UUID.randomUUID()}" +
            (if (extension.isNotEmpty()) ".$extension" else "")
        val url = storageService.upload(path, file.bytes, contentType)
        return ResponseEntity.status(HttpStatus.CREATED).body(UploadedImageResponse(url))
    }

    companion object {
        // 서버 전역 multipart 상한(20MB)보다 이미지용은 더 보수적으로.
        private const val MAX_IMAGE_BYTES = 10L * 1024 * 1024
    }
}
