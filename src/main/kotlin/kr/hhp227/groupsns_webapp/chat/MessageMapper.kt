package kr.hhp227.groupsns_webapp.chat

import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Options
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.Update

@Mapper
interface MessageMapper {
    @Select("SELECT message_id AS id, chat_room_id, user_id, message, created_at, deleted_at FROM messages WHERE message_id = #{id} AND deleted_at IS NULL")
    fun findById(id: Long): Message?

    @Insert(
        """
        INSERT INTO messages(chat_room_id, user_id, message, attachment_url, attachment_name, attachment_type, attachment_size)
        VALUES(#{chatRoomId}, #{userId}, #{message}, #{attachmentUrl}, #{attachmentName}, #{attachmentType}, #{attachmentSize})
        """
    )
    @Options(useGeneratedKeys = true, keyProperty = "id")
    fun insert(record: NewMessageRecord): Int

    @Update("UPDATE messages SET message = #{message} WHERE message_id = #{id} AND deleted_at IS NULL")
    fun update(update: MessageUpdate): Int

    @Update("UPDATE messages SET deleted_at = now() WHERE message_id = #{id} AND deleted_at IS NULL")
    fun softDelete(id: Long): Int

    @Select(
        """
        SELECT m.message_id AS id, m.chat_room_id, m.user_id, u.name AS author_name, u.profile_img AS author_profile_img,
               m.message, m.attachment_url, m.attachment_name, m.attachment_type, m.attachment_size, m.created_at
        FROM messages m
        JOIN users u ON u.id = m.user_id
        WHERE m.message_id = #{id} AND m.chat_room_id = #{chatRoomId} AND m.deleted_at IS NULL
        """
    )
    fun findFeedRowById(@Param("id") id: Long, @Param("chatRoomId") chatRoomId: Long): MessageFeedRow?

    // viewerId가 차단한 사용자의 메시지는 숨긴다(그룹 채팅/DM 공용 - 차단 숨김은 쿼리 레벨, 앱 DB 롤은 RLS 우회).
    @Select(
        """
        SELECT m.message_id AS id, m.chat_room_id, m.user_id, u.name AS author_name, u.profile_img AS author_profile_img,
               m.message, m.attachment_url, m.attachment_name, m.attachment_type, m.attachment_size, m.created_at
        FROM messages m
        JOIN users u ON u.id = m.user_id
        WHERE m.chat_room_id = #{chatRoomId} AND m.deleted_at IS NULL
          AND NOT EXISTS (SELECT 1 FROM user_blocks ub WHERE ub.blocker_id = #{viewerId} AND ub.blocked_id = m.user_id)
        ORDER BY m.created_at DESC
        LIMIT #{limit} OFFSET #{offset}
        """
    )
    fun findFeedByRoom(
        @Param("chatRoomId") chatRoomId: Long,
        @Param("viewerId") viewerId: Long,
        @Param("limit") limit: Int,
        @Param("offset") offset: Int
    ): List<MessageFeedRow>
}
