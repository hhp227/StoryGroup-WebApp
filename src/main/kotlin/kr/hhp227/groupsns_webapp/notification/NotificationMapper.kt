package kr.hhp227.groupsns_webapp.notification

import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Options
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.Update

// target 컨텍스트 역추적 조인 — REPLY는 user_replys로 게시글을 찾고, GROUP은 그룹 직결.
// 전부 LEFT JOIN: 게시글/그룹이 삭제됐거나 RLS(posts_select=멤버십)로 안 보이면 알림은 남기고 컨텍스트만 null.
private const val TARGET_CONTEXT_JOINS = """
    LEFT JOIN user_replys ur ON n.target_type = 'REPLY' AND ur.reply_id = n.target_id
    LEFT JOIN posts p ON p.deleted_at IS NULL AND p.id = CASE
        WHEN n.target_type = 'POST' THEN n.target_id
        WHEN n.target_type = 'REPLY' THEN ur.post_id
    END
    LEFT JOIN groups g ON g.deleted_at IS NULL AND g.id = COALESCE(
        p.group_id, CASE WHEN n.target_type = 'GROUP' THEN n.target_id END
    )
"""

private const val TARGET_CONTEXT_COLUMNS =
    "p.id AS post_id, LEFT(p.text, 60) AS post_preview, g.id AS group_id, g.name AS group_name"

@Mapper
interface NotificationMapper {
    @Insert(
        "INSERT INTO notifications(user_id, type, target_type, target_id) VALUES(#{userId}, #{type}, #{targetType}, #{targetId})"
    )
    @Options(useGeneratedKeys = true, keyProperty = "id")
    fun insert(record: NewNotificationRecord): Int

    @Select(
        """
        SELECT n.id, n.user_id, n.type, n.target_type, n.target_id, n.is_read, n.created_at, $TARGET_CONTEXT_COLUMNS
        FROM notifications n
        $TARGET_CONTEXT_JOINS
        WHERE n.id = #{id}
        """
    )
    fun findById(id: Long): Notification?

    @Select(
        """
        SELECT n.id, n.user_id, n.type, n.target_type, n.target_id, n.is_read, n.created_at, $TARGET_CONTEXT_COLUMNS
        FROM notifications n
        $TARGET_CONTEXT_JOINS
        WHERE n.user_id = #{userId}
        ORDER BY n.created_at DESC
        LIMIT #{limit} OFFSET #{offset}
        """
    )
    fun findByUser(@Param("userId") userId: Long, @Param("limit") limit: Int, @Param("offset") offset: Int): List<Notification>

    @Select("SELECT COUNT(*) FROM notifications WHERE user_id = #{userId} AND is_read = false")
    fun countUnread(userId: Long): Long

    @Update("UPDATE notifications SET is_read = true WHERE id = #{id} AND user_id = #{userId}")
    fun markAsRead(@Param("id") id: Long, @Param("userId") userId: Long): Int

    @Update("UPDATE notifications SET is_read = true WHERE user_id = #{userId} AND is_read = false")
    fun markAllAsRead(userId: Long): Int
}
