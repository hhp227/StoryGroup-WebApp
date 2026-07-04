package kr.hhp227.groupsns_webapp.chat

import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Options
import org.apache.ibatis.annotations.Select

@Mapper
interface ChatRoomMapper {
    @Select("SELECT chat_room_id AS id, group_id, name, created_at FROM chat_rooms WHERE chat_room_id = #{id}")
    fun findById(id: Long): ChatRoom?

    @Insert("INSERT INTO chat_rooms(group_id, name) VALUES(#{groupId}, #{name})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    fun insert(record: NewChatRoomRecord): Int

    @Select("SELECT chat_room_id AS id, group_id, name, created_at FROM chat_rooms WHERE group_id = #{groupId} ORDER BY created_at ASC")
    fun findByGroup(groupId: Long): List<ChatRoom>
}
