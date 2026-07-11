package kr.hhp227.groupsns_webapp.friend

import org.apache.ibatis.annotations.Delete
import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import java.time.OffsetDateTime

// 친구 목록 한 행 - 등록한 상대의 프로필을 함께 조회한다.
data class FriendRow(
    val userId: Long,
    val name: String,
    val profileImg: String?,
    val statusMessage: String?,
    val friendedAt: OffsetDateTime
)

@Mapper
interface UserFriendMapper {
    @Insert("INSERT INTO user_friends(user_id, friend_id) VALUES(#{userId}, #{friendId})")
    fun insert(@Param("userId") userId: Long, @Param("friendId") friendId: Long): Int

    @Delete("DELETE FROM user_friends WHERE user_id = #{userId} AND friend_id = #{friendId}")
    fun delete(@Param("userId") userId: Long, @Param("friendId") friendId: Long): Int

    @Select(
        """
        SELECT EXISTS (
            SELECT 1 FROM user_friends WHERE user_id = #{userId} AND friend_id = #{friendId}
        )
        """
    )
    fun exists(@Param("userId") userId: Long, @Param("friendId") friendId: Long): Boolean

    @Select(
        """
        SELECT uf.friend_id AS user_id, u.name, u.profile_img, u.status_message, uf.created_at AS friended_at
        FROM user_friends uf
        JOIN users u ON u.id = uf.friend_id
        WHERE uf.user_id = #{userId}
        ORDER BY u.name ASC
        """
    )
    fun findFriends(userId: Long): List<FriendRow>
}
