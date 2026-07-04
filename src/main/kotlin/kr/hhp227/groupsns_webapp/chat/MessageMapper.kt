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

    @Insert("INSERT INTO messages(chat_room_id, user_id, message) VALUES(#{chatRoomId}, #{userId}, #{message})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    fun insert(record: NewMessageRecord): Int

    @Update("UPDATE messages SET message = #{message} WHERE message_id = #{id} AND deleted_at IS NULL")
    fun update(update: MessageUpdate): Int

    @Update("UPDATE messages SET deleted_at = now() WHERE message_id = #{id} AND deleted_at IS NULL")
    fun softDelete(id: Long): Int

    @Select(
        """
        SELECT m.message_id AS id, m.chat_room_id, m.user_id, u.name AS author_name, u.profile_img AS author_profile_img,
               m.message, m.created_at
        FROM messages m
        JOIN users u ON u.id = m.user_id
        WHERE m.message_id = #{id} AND m.chat_room_id = #{chatRoomId} AND m.deleted_at IS NULL
        """
    )
    fun findFeedRowById(@Param("id") id: Long, @Param("chatRoomId") chatRoomId: Long): MessageFeedRow?

    @Select(
        """
        SELECT m.message_id AS id, m.chat_room_id, m.user_id, u.name AS author_name, u.profile_img AS author_profile_img,
               m.message, m.created_at
        FROM messages m
        JOIN users u ON u.id = m.user_id
        WHERE m.chat_room_id = #{chatRoomId} AND m.deleted_at IS NULL
        ORDER BY m.created_at DESC
        LIMIT #{limit} OFFSET #{offset}
        """
    )
    fun findFeedByRoom(@Param("chatRoomId") chatRoomId: Long, @Param("limit") limit: Int, @Param("offset") offset: Int): List<MessageFeedRow>
}
