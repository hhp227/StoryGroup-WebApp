package kr.hhp227.groupsns_webapp.chat

import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select

@Mapper
interface ChatRoomReadMapper {
    // GREATEST로 단조 증가만 허용 — 오래된 클라이언트(탭 여러 개 등)가 과거 위치로 되돌리지 못한다.
    @Insert(
        """
        INSERT INTO chat_room_reads(chat_room_id, user_id, last_read_message_id)
        VALUES(#{chatRoomId}, #{userId}, #{lastReadMessageId})
        ON CONFLICT (chat_room_id, user_id)
        DO UPDATE SET last_read_message_id = GREATEST(chat_room_reads.last_read_message_id, EXCLUDED.last_read_message_id),
                      updated_at = now()
        """
    )
    fun upsert(
        @Param("chatRoomId") chatRoomId: Long,
        @Param("userId") userId: Long,
        @Param("lastReadMessageId") lastReadMessageId: Long
    ): Int

    @Select("SELECT chat_room_id, user_id, last_read_message_id, updated_at FROM chat_room_reads WHERE chat_room_id = #{chatRoomId}")
    fun findByRoom(chatRoomId: Long): List<ChatRoomRead>

    @Select("SELECT last_read_message_id FROM chat_room_reads WHERE chat_room_id = #{chatRoomId} AND user_id = #{userId}")
    fun findPosition(@Param("chatRoomId") chatRoomId: Long, @Param("userId") userId: Long): Long?
}
