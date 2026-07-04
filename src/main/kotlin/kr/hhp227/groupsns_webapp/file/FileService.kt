package kr.hhp227.groupsns_webapp.file

import kr.hhp227.groupsns_webapp.common.db.DbSessionMapper
import kr.hhp227.groupsns_webapp.common.exception.ForbiddenException
import kr.hhp227.groupsns_webapp.common.exception.GroupFileNotFoundException
import kr.hhp227.groupsns_webapp.common.exception.GroupNotFoundException
import kr.hhp227.groupsns_webapp.file.dto.CreateFileRequest
import kr.hhp227.groupsns_webapp.file.dto.FileResponse
import kr.hhp227.groupsns_webapp.group.UserGroupMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FileService(
    private val fileMapper: FileMapper,
    private val userGroupMapper: UserGroupMapper,
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
        requireFileOwner(userId, groupId, fileId)
        fileMapper.softDelete(fileId)
    }

    private fun loadFile(fileId: Long, groupId: Long): FileResponse {
        val row = fileMapper.findFeedRowById(fileId, groupId) ?: throw GroupFileNotFoundException()
        return FileResponse.from(row)
    }

    private fun requireMembership(userId: Long, groupId: Long) {
        userGroupMapper.findRole(userId, groupId) ?: throw GroupNotFoundException()
    }

    private fun requireFileOwner(userId: Long, groupId: Long, fileId: Long) {
        val file = fileMapper.findById(fileId)?.takeIf { it.groupId == groupId } ?: throw GroupFileNotFoundException()
        if (file.userId != userId) throw ForbiddenException()
    }
}
