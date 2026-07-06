package kr.hhp227.groupsns_webapp.chat

import kr.hhp227.groupsns_webapp.chat.dto.ChatRoomResponse
import kr.hhp227.groupsns_webapp.chat.dto.CreateChatRoomRequest
import kr.hhp227.groupsns_webapp.chat.dto.CreateMessageRequest
import kr.hhp227.groupsns_webapp.chat.dto.DirectRoomResponse
import kr.hhp227.groupsns_webapp.chat.dto.MessageResponse
import kr.hhp227.groupsns_webapp.chat.dto.ReadPositionResponse
import kr.hhp227.groupsns_webapp.chat.dto.UpdateMessageRequest
import kr.hhp227.groupsns_webapp.common.db.DbSessionMapper
import kr.hhp227.groupsns_webapp.common.exception.ChatRoomNotFoundException
import kr.hhp227.groupsns_webapp.common.exception.ForbiddenException
import kr.hhp227.groupsns_webapp.common.exception.GroupNotFoundException
import kr.hhp227.groupsns_webapp.common.exception.MessageNotFoundException
import kr.hhp227.groupsns_webapp.group.UserGroupMapper
import kr.hhp227.groupsns_webapp.realtime.ChatSocketEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ChatService(
    private val chatRoomMapper: ChatRoomMapper,
    private val messageMapper: MessageMapper,
    private val chatRoomReadMapper: ChatRoomReadMapper,
    private val userGroupMapper: UserGroupMapper,
    private val dbSessionMapper: DbSessionMapper,
    // WebSocket 브로드캐스트는 ChatEventBroadcaster가 커밋 후에 처리 — 여기선 이벤트 발행만.
    private val eventPublisher: ApplicationEventPublisher
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
        return loadMessage(record.id, chatRoomId).also { eventPublisher.publishEvent(ChatSocketEvent.created(it)) }
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
        return loadMessage(messageId, chatRoomId).also { eventPublisher.publishEvent(ChatSocketEvent.updated(it)) }
    }

    @Transactional
    fun deleteMessage(userId: Long, groupId: Long, chatRoomId: Long, messageId: Long) {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        requireChatRoomExists(groupId, chatRoomId)
        requireMessageOwner(userId, chatRoomId, messageId)
        messageMapper.softDelete(messageId)
        eventPublisher.publishEvent(ChatSocketEvent.deleted(chatRoomId, messageId))
    }

    @Transactional
    fun getOrCreateDirectRoom(userId: Long, otherUserId: Long): ChatRoomResponse {
        if (userId == otherUserId) throw IllegalArgumentException("자기 자신과는 DM을 시작할 수 없습니다")
        dbSessionMapper.setCurrentUserId(userId)

        val existing = chatRoomMapper.findDirectRoom(userId, otherUserId)
        if (existing != null) return ChatRoomResponse.from(existing)

        val record = NewDirectRoomRecord(userId, otherUserId, "DM")
        chatRoomMapper.insertDirect(record)
        val room = chatRoomMapper.findById(record.id) ?: throw ChatRoomNotFoundException()
        return ChatRoomResponse.from(room)
    }

    @Transactional
    fun listDirectRooms(userId: Long): List<DirectRoomResponse> {
        dbSessionMapper.setCurrentUserId(userId)
        return chatRoomMapper.findDirectRoomsForUser(userId).map { DirectRoomResponse.from(it) }
    }

    @Transactional
    fun sendDirectMessage(userId: Long, chatRoomId: Long, request: CreateMessageRequest): MessageResponse {
        dbSessionMapper.setCurrentUserId(userId)
        requireDirectRoomParticipant(userId, chatRoomId)

        val record = NewMessageRecord(chatRoomId, userId, request.text)
        messageMapper.insert(record)
        return loadMessage(record.id, chatRoomId).also { eventPublisher.publishEvent(ChatSocketEvent.created(it)) }
    }

    @Transactional
    fun listDirectMessages(userId: Long, chatRoomId: Long, page: Int, size: Int): List<MessageResponse> {
        dbSessionMapper.setCurrentUserId(userId)
        requireDirectRoomParticipant(userId, chatRoomId)
        return messageMapper.findFeedByRoom(chatRoomId, size, page * size).map { MessageResponse.from(it) }
    }

    @Transactional
    fun deleteDirectMessage(userId: Long, chatRoomId: Long, messageId: Long) {
        dbSessionMapper.setCurrentUserId(userId)
        requireDirectRoomParticipant(userId, chatRoomId)
        requireMessageOwner(userId, chatRoomId, messageId)
        messageMapper.softDelete(messageId)
        eventPublisher.publishEvent(ChatSocketEvent.deleted(chatRoomId, messageId))
    }

    @Transactional
    fun markRead(userId: Long, groupId: Long, chatRoomId: Long, lastReadMessageId: Long) {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        requireChatRoomExists(groupId, chatRoomId)
        doMarkRead(userId, chatRoomId, lastReadMessageId)
    }

    @Transactional
    fun listReads(userId: Long, groupId: Long, chatRoomId: Long): List<ReadPositionResponse> {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        requireChatRoomExists(groupId, chatRoomId)
        return chatRoomReadMapper.findByRoom(chatRoomId).map { ReadPositionResponse.from(it) }
    }

    @Transactional
    fun markDirectRead(userId: Long, chatRoomId: Long, lastReadMessageId: Long) {
        dbSessionMapper.setCurrentUserId(userId)
        requireDirectRoomParticipant(userId, chatRoomId)
        doMarkRead(userId, chatRoomId, lastReadMessageId)
    }

    @Transactional
    fun listDirectReads(userId: Long, chatRoomId: Long): List<ReadPositionResponse> {
        dbSessionMapper.setCurrentUserId(userId)
        requireDirectRoomParticipant(userId, chatRoomId)
        return chatRoomReadMapper.findByRoom(chatRoomId).map { ReadPositionResponse.from(it) }
    }

    private fun doMarkRead(userId: Long, chatRoomId: Long, lastReadMessageId: Long) {
        // 읽음 위치는 이 방의 실존 메시지여야 한다 — 다른 방 메시지 id로 위치를 오염시키는 것 방지.
        messageMapper.findById(lastReadMessageId)?.takeIf { it.chatRoomId == chatRoomId }
            ?: throw MessageNotFoundException()
        chatRoomReadMapper.upsert(chatRoomId, userId, lastReadMessageId)
        // 방송은 요청 값이 아니라 GREATEST 적용 후의 실제 위치로 — 뒤늦은 요청이 과거 위치를 방송하지 않게.
        val position = chatRoomReadMapper.findPosition(chatRoomId, userId) ?: lastReadMessageId
        eventPublisher.publishEvent(ChatSocketEvent.read(chatRoomId, userId, position))
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

    private fun requireDirectRoomParticipant(userId: Long, chatRoomId: Long) {
        val room = chatRoomMapper.findById(chatRoomId)?.takeIf { it.groupId == null } ?: throw ChatRoomNotFoundException()
        if (room.userAId != userId && room.userBId != userId) throw ChatRoomNotFoundException()
    }
}
