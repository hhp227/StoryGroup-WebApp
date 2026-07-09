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

data class UploadedVideoResponse(val url: String)

// /api/images의 동영상판 — 게시글 videos처럼 "URL 문자열을 받는 API"에 넣을 동영상을 올린다.
// 크기 상한은 서버 전역 multipart 설정(20MB)에 맡긴다 — Cloud Run HTTP/1 요청 상한(32MB) 안쪽이라
// 더 키우려면 multipart 설정만으로는 안 되고 스토리지 직접 업로드(서명 URL) 방식이 필요하다.
@RestController
@RequestMapping("/api/videos")
class VideoUploadController(private val storageService: StorageService) {

    @PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(
        @AuthenticationPrincipal principal: UserPrincipal,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<UploadedVideoResponse> {
        if (file.isEmpty) throw IllegalArgumentException("빈 파일은 업로드할 수 없습니다")
        val contentType = file.contentType ?: ""
        if (!contentType.startsWith("video/")) throw IllegalArgumentException("동영상 파일만 업로드할 수 있습니다")

        // FileService.uploadBinaryFile과 같은 규칙 — 저장 이름은 UUID(+확장자)로 특수문자/한글 문제 회피.
        val extension = (file.originalFilename ?: "")
            .substringAfterLast('.', "").filter { it.isLetterOrDigit() }.take(10)
        val path = "videos/user-${principal.id}/${UUID.randomUUID()}" +
            (if (extension.isNotEmpty()) ".$extension" else "")
        val url = storageService.upload(path, file.bytes, contentType)
        return ResponseEntity.status(HttpStatus.CREATED).body(UploadedVideoResponse(url))
    }
}
