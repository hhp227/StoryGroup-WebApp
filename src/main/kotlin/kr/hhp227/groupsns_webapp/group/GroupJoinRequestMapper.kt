package kr.hhp227.groupsns_webapp.group

import org.apache.ibatis.annotations.Delete
import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select

@Mapper
interface GroupJoinRequestMapper {
    @Insert("INSERT INTO group_join_requests(group_id, user_id) VALUES(#{groupId}, #{userId})")
    fun insert(@Param("groupId") groupId: Long, @Param("userId") userId: Long): Int

    // 본인 취소와 모더레이터의 승인/거절 처리가 공용으로 쓴다 - 반환값 0이면 신청이 없던 것.
    @Delete("DELETE FROM group_join_requests WHERE group_id = #{groupId} AND user_id = #{userId}")
    fun delete(@Param("groupId") groupId: Long, @Param("userId") userId: Long): Int

    @Select(
        """
        SELECT EXISTS (
            SELECT 1 FROM group_join_requests WHERE group_id = #{groupId} AND user_id = #{userId}
        )
        """
    )
    fun exists(@Param("groupId") groupId: Long, @Param("userId") userId: Long): Boolean

    @Select(
        """
        SELECT jr.user_id, u.name, u.profile_img, jr.created_at
        FROM group_join_requests jr
        JOIN users u ON u.id = jr.user_id
        WHERE jr.group_id = #{groupId}
        ORDER BY jr.created_at ASC
        """
    )
    fun findByGroup(groupId: Long): List<GroupJoinRequestRow>
}
