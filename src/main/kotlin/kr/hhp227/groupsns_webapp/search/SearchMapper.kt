package kr.hhp227.groupsns_webapp.search

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select

// 모든 쿼리가 user_groups 조인으로 "내가 속한 그룹" 범위로 제한된다(RLS는 2차 방어선).
// 검색어는 서비스 레이어에서 LIKE 와일드카드(%,_)를 이스케이프한 뒤 넘어온다.
@Mapper
interface SearchMapper {
    @Select(
        """
        SELECT g.id, g.name, g.image, g.description
        FROM groups g
        JOIN user_groups ug ON ug.group_id = g.id AND ug.user_id = #{userId}
        WHERE g.deleted_at IS NULL
          AND g.name ILIKE '%' || #{query} || '%'
        ORDER BY g.created_at DESC
        LIMIT #{limit}
        """
    )
    fun searchGroups(@Param("userId") userId: Long, @Param("query") query: String, @Param("limit") limit: Int): List<GroupSearchRow>

    @Select(
        """
        SELECT p.id, p.group_id, g.name AS group_name, u.name AS author_name, p.text, p.created_at
        FROM posts p
        JOIN groups g ON g.id = p.group_id AND g.deleted_at IS NULL
        JOIN user_groups ug ON ug.group_id = p.group_id AND ug.user_id = #{userId}
        JOIN users u ON u.id = p.user_id
        WHERE p.deleted_at IS NULL
          AND p.text ILIKE '%' || #{query} || '%'
        ORDER BY p.created_at DESC
        LIMIT #{limit}
        """
    )
    fun searchPosts(@Param("userId") userId: Long, @Param("query") query: String, @Param("limit") limit: Int): List<PostSearchRow>

    @Select(
        """
        SELECT f.id, f.group_id, g.name AS group_name, f.name, f.url, f.created_at
        FROM files f
        JOIN groups g ON g.id = f.group_id AND g.deleted_at IS NULL
        JOIN user_groups ug ON ug.group_id = f.group_id AND ug.user_id = #{userId}
        WHERE f.deleted_at IS NULL
          AND f.name ILIKE '%' || #{query} || '%'
        ORDER BY f.created_at DESC
        LIMIT #{limit}
        """
    )
    fun searchFiles(@Param("userId") userId: Long, @Param("query") query: String, @Param("limit") limit: Int): List<FileSearchRow>

    // 그룹 채팅방(멤버인 그룹, 그룹 미삭제)과 내가 참가자인 DM방을 함께 검색한다.
    @Select(
        """
        SELECT m.message_id AS id, m.chat_room_id, r.group_id, g.name AS group_name,
               u.name AS author_name, m.message, m.created_at
        FROM messages m
        JOIN chat_rooms r ON r.chat_room_id = m.chat_room_id
        LEFT JOIN groups g ON g.id = r.group_id AND g.deleted_at IS NULL
        JOIN users u ON u.id = m.user_id
        WHERE m.deleted_at IS NULL
          AND m.message ILIKE '%' || #{query} || '%'
          AND (
            (r.group_id IS NOT NULL AND g.id IS NOT NULL AND EXISTS (
                SELECT 1 FROM user_groups ug WHERE ug.group_id = r.group_id AND ug.user_id = #{userId}
            ))
            OR (r.group_id IS NULL AND (r.user_a_id = #{userId} OR r.user_b_id = #{userId}))
          )
        ORDER BY m.created_at DESC
        LIMIT #{limit}
        """
    )
    fun searchMessages(@Param("userId") userId: Long, @Param("query") query: String, @Param("limit") limit: Int): List<MessageSearchRow>

    // 폐쇄형 SNS 특성상 전체 사용자 검색이 아니라 "나와 같은 그룹에 속한" 사용자만 노출한다(자신 제외).
    @Select(
        """
        SELECT DISTINCT u.id, u.name, u.profile_img, u.status_message
        FROM users u
        JOIN user_groups ug ON ug.user_id = u.id
        JOIN user_groups mine ON mine.group_id = ug.group_id AND mine.user_id = #{userId}
        WHERE u.id != #{userId} AND u.deleted_at IS NULL
          AND u.name ILIKE '%' || #{query} || '%'
        ORDER BY u.name ASC
        LIMIT #{limit}
        """
    )
    fun searchUsers(@Param("userId") userId: Long, @Param("query") query: String, @Param("limit") limit: Int): List<UserSearchRow>
}
