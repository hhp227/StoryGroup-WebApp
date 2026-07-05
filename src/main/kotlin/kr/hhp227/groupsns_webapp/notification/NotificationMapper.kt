package kr.hhp227.groupsns_webapp.notification

import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.Update

@Mapper
interface NotificationMapper {
    @Insert(
        "INSERT INTO notifications(user_id, type, target_type, target_id) VALUES(#{userId}, #{type}, #{targetType}, #{targetId})"
    )
    fun insert(record: NewNotificationRecord): Int

    @Select(
        """
        SELECT id, user_id, type, target_type, target_id, is_read, created_at
        FROM notifications
        WHERE user_id = #{userId}
        ORDER BY created_at DESC
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
