package kr.hhp227.groupsns_webapp.file

import kr.hhp227.groupsns_webapp.common.db.DbSessionMapper
import kr.hhp227.groupsns_webapp.common.exception.ForbiddenException
import kr.hhp227.groupsns_webapp.common.exception.GroupFileNotFoundException
import kr.hhp227.groupsns_webapp.common.exception.GroupNotFoundException
import kr.hhp227.groupsns_webapp.common.storage.StorageService
import kr.hhp227.groupsns_webapp.file.dto.CreateFileRequest
import kr.hhp227.groupsns_webapp.file.dto.FileResponse
import kr.hhp227.groupsns_webapp.group.UserGroupMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Service
class FileService(
    private val fileMapper: FileMapper,
    private val userGroupMapper: UserGroupMapper,
    private val storageService: StorageService,
    private val dbSessionMapper: DbSessionMapper
) {
    @Transactional
    fun uploadFile(userId: Long, groupId: Long, request: CreateFileRequest): FileResponse {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)

        val record = NewFileRecord(groupId, userId, request.name, request.url, request.size, request.contentType)
        fileMapper.insert(record)
        return loadFile(record.id, groupId)
    }

    // 실제 바이너리 업로드(Supabase Storage 경유). 저장 경로는 원본 파일명 대신
    // UUID(+확장자)를 써서 특수문자/한글 파일명 문제를 피하고, 원본 이름은 메타데이터로만 남긴다.
    @Transactional
    fun uploadBinaryFile(userId: Long, groupId: Long, file: MultipartFile): FileResponse {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        if (file.isEmpty) throw IllegalArgumentException("빈 파일은 업로드할 수 없습니다")

        val originalName = file.originalFilename?.takeIf { it.isNotBlank() } ?: "파일"
        val extension = originalName.substringAfterLast('.', "").filter { it.isLetterOrDigit() }.take(10)
        val path = "group-$groupId/${UUID.randomUUID()}" + (if (extension.isNotEmpty()) ".$extension" else "")
        val url = storageService.upload(path, file.bytes, file.contentType)

        val record = NewFileRecord(groupId, userId, originalName.take(255), url, file.size, file.contentType?.take(100))
        fileMapper.insert(record)
        return loadFile(record.id, groupId)
    }

    @Transactional
    fun listFiles(userId: Long, groupId: Long, page: Int, size: Int): List<FileResponse> {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        return fileMapper.findFeedByGroup(groupId, size, page * size).map { FileResponse.from(it) }
    }

    @Transactional
    fun getFile(userId: Long, groupId: Long, fileId: Long): FileResponse {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        return loadFile(fileId, groupId)
    }

    @Transactional
    fun deleteFile(userId: Long, groupId: Long, fileId: Long) {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        val file = requireFileOwner(userId, groupId, fileId)
        fileMapper.softDelete(fileId)
        // 메타데이터 삭제 후 실제 스토리지 객체도 정리(우리 버킷 URL일 때만, 실패해도 무시)
        storageService.deleteByPublicUrl(file.url)
    }

    private fun loadFile(fileId: Long, groupId: Long): FileResponse {
        val row = fileMapper.findFeedRowById(fileId, groupId) ?: throw GroupFileNotFoundException()
        return FileResponse.from(row)
    }

    private fun requireMembership(userId: Long, groupId: Long) {
        userGroupMapper.findRole(userId, groupId) ?: throw GroupNotFoundException()
    }

    private fun requireFileOwner(userId: Long, groupId: Long, fileId: Long): GroupFile {
        val file = fileMapper.findById(fileId)?.takeIf { it.groupId == groupId } ?: throw GroupFileNotFoundException()
        if (file.userId != userId) throw ForbiddenException()
        return file
    }
}
