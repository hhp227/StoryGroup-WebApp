package kr.hhp227.groupsns_webapp.chat

import kr.hhp227.groupsns_webapp.chat.dto.ChatRoomResponse
import kr.hhp227.groupsns_webapp.chat.dto.CreateChatRoomRequest
import kr.hhp227.groupsns_webapp.chat.dto.CreateMessageRequest
import kr.hhp227.groupsns_webapp.chat.dto.DirectRoomResponse
import kr.hhp227.groupsns_webapp.chat.dto.GroupChatRoomResponse
import kr.hhp227.groupsns_webapp.chat.dto.MessageResponse
import kr.hhp227.groupsns_webapp.chat.dto.ReadPositionResponse
import kr.hhp227.groupsns_webapp.chat.dto.UpdateMessageRequest
import kr.hhp227.groupsns_webapp.block.UserBlockMapper
import kr.hhp227.groupsns_webapp.common.db.DbSessionMapper
import kr.hhp227.groupsns_webapp.common.exception.BlockedUserException
import kr.hhp227.groupsns_webapp.common.exception.ChatRoomNotFoundException
import kr.hhp227.groupsns_webapp.common.exception.ForbiddenException
import kr.hhp227.groupsns_webapp.common.exception.GroupNotFoundException
import kr.hhp227.groupsns_webapp.common.exception.MessageNotFoundException
import kr.hhp227.groupsns_webapp.group.GroupMapper
import kr.hhp227.groupsns_webapp.group.UserGroupMapper
import kr.hhp227.groupsns_webapp.realtime.ChatBadgeSocketEvent
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
    private val groupMapper: GroupMapper,
    private val userBlockMapper: UserBlockMapper,
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

        val record = buildNewMessage(chatRoomId, userId, request)
        messageMapper.insert(record)
        val message = loadMessage(record.id, chatRoomId)
        publishGroupBadgeEvents(userId, groupId, message)
        return message.also { eventPublisher.publishEvent(ChatSocketEvent.created(it)) }
    }

    @Transactional
    fun listMessages(userId: Long, groupId: Long, chatRoomId: Long, page: Int, size: Int): List<MessageResponse> {
        dbSessionMapper.setCurrentUserId(userId)
        requireMembership(userId, groupId)
        requireChatRoomExists(groupId, chatRoomId)
        return messageMapper.findFeedByRoom(chatRoomId, userId, size, page * size).map { MessageResponse.from(it) }
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
        requireNotBlocked(userId, otherUserId)

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

    // 채팅 허브(웹 /dm): 내가 속한 모든 그룹의 채팅방. 멤버십은 조인 조건이 담당해 별도 검사 불필요.
    @Transactional
    fun listMyGroupChatRooms(userId: Long): List<GroupChatRoomResponse> {
        dbSessionMapper.setCurrentUserId(userId)
        return chatRoomMapper.findGroupRoomsForUser(userId).map { GroupChatRoomResponse.from(it) }
    }

    @Transactional
    fun sendDirectMessage(userId: Long, chatRoomId: Long, request: CreateMessageRequest): MessageResponse {
        dbSessionMapper.setCurrentUserId(userId)
        val room = requireDirectRoomParticipant(userId, chatRoomId)
        requireNotBlocked(userId, if (room.userAId == userId) room.userBId!! else room.userAId!!)

        val record = buildNewMessage(chatRoomId, userId, request)
        messageMapper.insert(record)
        val message = loadMessage(record.id, chatRoomId)
        val otherUserId = if (room.userAId == userId) room.userBId!! else room.userAId!!
        eventPublisher.publishEvent(badgeEvent(otherUserId, message))
        return message.also { eventPublisher.publishEvent(ChatSocketEvent.created(it)) }
    }

    @Transactional
    fun listDirectMessages(userId: Long, chatRoomId: Long, page: Int, size: Int): List<MessageResponse> {
        dbSessionMapper.setCurrentUserId(userId)
        requireDirectRoomParticipant(userId, chatRoomId)
        return messageMapper.findFeedByRoom(chatRoomId, userId, size, page * size).map { MessageResponse.from(it) }
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

    // 그룹 채팅/DM 공용. 첨부만 있는 메시지는 text를 빈 문자열로 저장한다(V15 체크 제약과 짝).
    private fun buildNewMessage(chatRoomId: Long, userId: Long, request: CreateMessageRequest): NewMessageRecord {
        val text = request.text?.takeIf { it.isNotBlank() } ?: ""
        val attachment = request.attachment
        if (text.isEmpty() && attachment == null) {
            throw IllegalArgumentException("메시지 내용이나 첨부 파일이 필요합니다")
        }
        return NewMessageRecord(
            chatRoomId = chatRoomId,
            userId = userId,
            message = text,
            attachmentUrl = attachment?.url,
            attachmentName = attachment?.name?.take(255),
            attachmentType = attachment?.contentType?.take(100),
            attachmentSize = attachment?.size
        )
    }

    // 그룹방 새 메시지의 뱃지 수신자 = 그룹 멤버 전원 - 발신자 - 발신자를 차단한 사용자.
    // 라운지는 전원 자동 가입이라 허브 목록에서도 빠져 있으므로(NotificationService의 라운지 제외와 같은 이유) 보내지 않는다.
    private fun publishGroupBadgeEvents(senderId: Long, groupId: Long, message: MessageResponse) {
        val group = groupMapper.findById(groupId) ?: return
        if (group.isLounge) return
        userGroupMapper.findMembers(groupId)
            .asSequence()
            .map { it.userId }
            .filter { it != senderId && !userBlockMapper.exists(it, senderId) }
            .forEach { eventPublisher.publishEvent(badgeEvent(it, message, groupId, group.name)) }
    }

    // 뱃지 수신자는 발신자를 차단한 사용자가 이미 걸러져 있어(그룹은 위 필터, DM은 전송 차단)
    // 미리보기 본문을 실어도 목록 REST(last_message_*)와 가시성이 어긋나지 않는다.
    private fun badgeEvent(
        recipientId: Long,
        message: MessageResponse,
        groupId: Long? = null,
        roomName: String? = null
    ) = ChatBadgeSocketEvent(
        recipientId = recipientId,
        chatRoomId = message.chatRoomId,
        messageId = message.id,
        senderId = message.userId,
        senderName = message.authorName,
        roomName = roomName,
        groupId = groupId,
        text = message.text,
        attachmentType = message.attachment?.contentType,
        createdAt = message.createdAt
    )

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

    private fun requireDirectRoomParticipant(userId: Long, chatRoomId: Long): ChatRoom {
        val room = chatRoomMapper.findById(chatRoomId)?.takeIf { it.groupId == null } ?: throw ChatRoomNotFoundException()
        if (room.userAId != userId && room.userBId != userId) throw ChatRoomNotFoundException()
        return room
    }

    // DM은 어느 쪽이 차단했든 양방향으로 막는다. 누가 차단했는지는 응답에 노출하지 않는다.
    private fun requireNotBlocked(userId: Long, otherUserId: Long) {
        if (userBlockMapper.existsBetween(userId, otherUserId)) throw BlockedUserException()
    }
}
