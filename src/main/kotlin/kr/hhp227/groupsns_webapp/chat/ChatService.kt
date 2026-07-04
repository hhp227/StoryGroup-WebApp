package kr.hhp227.groupsns_webapp.chat

import kr.hhp227.groupsns_webapp.chat.dto.ChatRoomResponse
import kr.hhp227.groupsns_webapp.chat.dto.CreateChatRoomRequest
import kr.hhp227.groupsns_webapp.chat.dto.CreateMessageRequest
import kr.hhp227.groupsns_webapp.chat.dto.MessageResponse
import kr.hhp227.groupsns_webapp.chat.dto.UpdateMessageRequest
import kr.hhp227.groupsns_webapp.common.db.DbSessionMapper
import kr.hhp227.groupsns_webapp.common.exception.ChatRoomNotFoundException
import kr.hhp227.groupsns_webapp.common.exception.ForbiddenException
import kr.hhp227.groupsns_webapp.common.exception.GroupNotFoundException
import kr.hhp227.groupsns_webapp.common.exception.MessageNotFoundException
import kr.hhp227.groupsns_webapp.group.UserGroupMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChatService(
    private val chatRoomMapper: ChatRoomMapper,
    private val messageMapper: MessageMapper,
    private val userGroupMapper: UserGroupMapper,
    private val dbSessionMapper: DbSessionMapper
) {
    @Transactional
    fun createChatRoom(userId: Long, groupId: Long, request: CreateChatRoomRequest): ChatRoomResponse {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)

        val record = NewChatRoomRecord(groupId, request.name)
        chatRoomMapper.insert(record)
        val room = chatRoomMapper.findById(record.id) ?: throw ChatRoomNotFoundException()
        return ChatRoomResponse.from(room)
    }

    @Transactional
    fun listChatRooms(userId: Long, groupId: Long): List<ChatRoomResponse> {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        return chatRoomMapper.findByGroup(groupId).map { ChatRoomResponse.from(it) }
    }

    @Transactional
    fun sendMessage(userId: Long, groupId: Long, chatRoomId: Long, request: CreateMessageRequest): MessageResponse {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        requireChatRoomExists(groupId, chatRoomId)

        val record = NewMessageRecord(chatRoomId, userId, request.text)
        messageMapper.insert(record)
        return loadMessage(record.id, chatRoomId)
    }

    @Transactional
    fun listMessages(userId: Long, groupId: Long, chatRoomId: Long, page: Int, size: Int): List<MessageResponse> {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        requireChatRoomExists(groupId, chatRoomId)
        return messageMapper.findFeedByRoom(chatRoomId, size, page * size).map { MessageResponse.from(it) }
    }

    @Transactional
    fun updateMessage(userId: Long, groupId: Long, chatRoomId: Long, messageId: Long, request: UpdateMessageRequest): MessageResponse {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        requireChatRoomExists(groupId, chatRoomId)
        requireMessageOwner(userId, chatRoomId, messageId)

        val updated = messageMapper.update(MessageUpdate(messageId, request.text))
        if (updated == 0) throw MessageNotFoundException()
        return loadMessage(messageId, chatRoomId)
    }

    @Transactional
    fun deleteMessage(userId: Long, groupId: Long, chatRoomId: Long, messageId: Long) {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        requireChatRoomExists(groupId, chatRoomId)
        requireMessageOwner(userId, chatRoomId, messageId)
        messageMapper.softDelete(messageId)
    }

    private fun loadMessage(messageId: Long, chatRoomId: Long): MessageResponse {
        val row = messageMapper.findFeedRowById(messageId, chatRoomId) ?: throw MessageNotFoundException()
        return MessageResponse.from(row)
    }

    private fun requireMembership(userId: Long, groupId: Long) {
        userGroupMapper.findRole(userId, groupId) ?: throw GroupNotFoundException()
    }

    private fun requireChatRoomExists(groupId: Long, chatRoomId: Long) {
        chatRoomMapper.findById(chatRoomId)?.takeIf { it.groupId == groupId } ?: throw ChatRoomNotFoundException()
    }

    private fun requireMessageOwner(userId: Long, chatRoomId: Long, messageId: Long) {
        val message = messageMapper.findById(messageId)?.takeIf { it.chatRoomId == chatRoomId } ?: throw MessageNotFoundException()
        if (message.userId != userId) throw ForbiddenException()
    }
}
