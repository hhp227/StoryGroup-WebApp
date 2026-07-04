package kr.hhp227.groupsns_webapp.file

import kr.hhp227.groupsns_webapp.file.dto.CreateFileRequest
import kr.hhp227.groupsns_webapp.file.dto.FileResponse
import kr.hhp227.groupsns_webapp.security.UserPrincipal
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid

@RestController
@RequestMapping("/api/groups/{groupId}/files")
class FileController(private val fileService: FileService) {

    @PostMapping
    fun uploadFile(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @Valid @RequestBody request: CreateFileRequest
    ): ResponseEntity<FileResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(fileService.uploadFile(principal.id, groupId, request))

    @GetMapping
    fun listFiles(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): List<FileResponse> = fileService.listFiles(principal.id, groupId, page, size.coerceIn(1, 50))

    @GetMapping("/{fileId}")
    fun getFile(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable fileId: Long
    ): FileResponse = fileService.getFile(principal.id, groupId, fileId)

    @DeleteMapping("/{fileId}")
    fun deleteFile(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable groupId: Long,
        @PathVariable fileId: Long
    ): ResponseEntity<Void> {
        fileService.deleteFile(principal.id, groupId, fileId)
        return ResponseEntity.noContent().build()
    }
}
