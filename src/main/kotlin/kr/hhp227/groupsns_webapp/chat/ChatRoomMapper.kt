package kr.hhp227.groupsns_webapp.chat

import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Options
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select

@Mapper
interface ChatRoomMapper {
    @Select("SELECT chat_room_id AS id, group_id, name, created_at, user_a_id, user_b_id FROM chat_rooms WHERE chat_room_id = #{id}")
    fun findById(id: Long): ChatRoom?

    @Insert("INSERT INTO chat_rooms(group_id, name) VALUES(#{groupId}, #{name})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    fun insert(record: NewChatRoomRecord): Int

    @Select("SELECT chat_room_id AS id, group_id, name, created_at, user_a_id, user_b_id FROM chat_rooms WHERE group_id = #{groupId} ORDER BY created_at ASC")
    fun findByGroup(groupId: Long): List<ChatRoom>

    // 채팅 허브(웹 /dm): 내가 속한 모든 그룹의 채팅방을 그룹명과 함께. 라운지 → 그룹명 → 방 생성 순.
    @Select(
        """
        SELECT r.chat_room_id AS id, r.group_id, g.name AS group_name, g.is_lounge, r.name, r.created_at
        FROM chat_rooms r
        JOIN groups g ON g.id = r.group_id
        JOIN user_groups ug ON ug.group_id = r.group_id AND ug.user_id = #{userId}
        ORDER BY g.is_lounge DESC, g.name ASC, r.created_at ASC
        """
    )
    fun findGroupRoomsForUser(userId: Long): List<GroupChatRoomRow>

    // 두 사람 순서와 무관하게 유니크 인덱스(LEAST/GREATEST)를 그대로 타서 기존 방을 찾는다.
    @Select(
        """
        SELECT chat_room_id AS id, group_id, name, created_at, user_a_id, user_b_id
        FROM chat_rooms
        WHERE group_id IS NULL
          AND LEAST(user_a_id, user_b_id) = LEAST(#{userAId}, #{userBId})
          AND GREATEST(user_a_id, user_b_id) = GREATEST(#{userAId}, #{userBId})
        """
    )
    fun findDirectRoom(@Param("userAId") userAId: Long, @Param("userBId") userBId: Long): ChatRoom?

    @Insert("INSERT INTO chat_rooms(user_a_id, user_b_id, name) VALUES(#{userAId}, #{userBId}, #{name})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    fun insertDirect(record: NewDirectRoomRecord): Int

    @Select(
        """
        SELECT r.chat_room_id AS id,
               CASE WHEN r.user_a_id = #{userId} THEN r.user_b_id ELSE r.user_a_id END AS other_user_id,
               u.name AS other_user_name, u.profile_img AS other_user_profile_img,
               r.created_at
        FROM chat_rooms r
        JOIN users u ON u.id = CASE WHEN r.user_a_id = #{userId} THEN r.user_b_id ELSE r.user_a_id END
        WHERE r.group_id IS NULL AND (r.user_a_id = #{userId} OR r.user_b_id = #{userId})
        ORDER BY r.created_at DESC
        """
    )
    fun findDirectRoomsForUser(userId: Long): List<DirectRoomRow>
}
