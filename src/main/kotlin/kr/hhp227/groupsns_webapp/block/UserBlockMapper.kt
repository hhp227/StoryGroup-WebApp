package kr.hhp227.groupsns_webapp.block

import org.apache.ibatis.annotations.Delete
import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import java.time.OffsetDateTime

// 차단 목록 한 행 - 차단한 상대의 프로필을 함께 조회한다.
data class BlockedUserRow(
    val userId: Long,
    val name: String,
    val profileImg: String?,
    val blockedAt: OffsetDateTime
)

@Mapper
interface UserBlockMapper {
    @Insert("INSERT INTO user_blocks(blocker_id, blocked_id) VALUES(#{blockerId}, #{blockedId})")
    fun insert(@Param("blockerId") blockerId: Long, @Param("blockedId") blockedId: Long): Int

    @Delete("DELETE FROM user_blocks WHERE blocker_id = #{blockerId} AND blocked_id = #{blockedId}")
    fun delete(@Param("blockerId") blockerId: Long, @Param("blockedId") blockedId: Long): Int

    @Select(
        """
        SELECT EXISTS (
            SELECT 1 FROM user_blocks WHERE blocker_id = #{blockerId} AND blocked_id = #{blockedId}
        )
        """
    )
    fun exists(@Param("blockerId") blockerId: Long, @Param("blockedId") blockedId: Long): Boolean

    // DM 차단 검사용 - 어느 쪽이 차단했든 true.
    @Select(
        """
        SELECT EXISTS (
            SELECT 1 FROM user_blocks
            WHERE (blocker_id = #{a} AND blocked_id = #{b}) OR (blocker_id = #{b} AND blocked_id = #{a})
        )
        """
    )
    fun existsBetween(@Param("a") a: Long, @Param("b") b: Long): Boolean

    @Select(
        """
        SELECT ub.blocked_id AS user_id, u.name, u.profile_img, ub.created_at AS blocked_at
        FROM user_blocks ub
        JOIN users u ON u.id = ub.blocked_id
        WHERE ub.blocker_id = #{blockerId}
        ORDER BY ub.created_at DESC
        """
    )
    fun findBlockedUsers(blockerId: Long): List<BlockedUserRow>
}
