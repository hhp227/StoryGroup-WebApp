package kr.hhp227.groupsns_webapp.group

import org.apache.ibatis.annotations.Delete
import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.Update

@Mapper
interface UserGroupMapper {
    @Insert("INSERT INTO user_groups(user_id, group_id, role) VALUES(#{userId}, #{groupId}, #{role})")
    fun insert(
        @Param("userId") userId: Long,
        @Param("groupId") groupId: Long,
        @Param("role") role: GroupRole
    ): Int

    @Select("SELECT role FROM user_groups WHERE user_id = #{userId} AND group_id = #{groupId}")
    fun findRole(@Param("userId") userId: Long, @Param("groupId") groupId: Long): GroupRole?

    @Update("UPDATE user_groups SET role = #{role} WHERE user_id = #{userId} AND group_id = #{groupId}")
    fun updateRole(
        @Param("userId") userId: Long,
        @Param("groupId") groupId: Long,
        @Param("role") role: GroupRole
    ): Int

    @Delete("DELETE FROM user_groups WHERE user_id = #{userId} AND group_id = #{groupId}")
    fun delete(@Param("userId") userId: Long, @Param("groupId") groupId: Long): Int

    @Select(
        """
        SELECT g.id, g.author_id, g.name, g.image, g.description, g.join_type, g.created_at, g.deleted_at,
               ug.role AS my_role, g.is_lounge
        FROM groups g
        JOIN user_groups ug ON ug.group_id = g.id
        WHERE ug.user_id = #{userId} AND g.deleted_at IS NULL
        ORDER BY g.is_lounge DESC, g.created_at DESC
        """
    )
    fun findGroupsForUser(userId: Long): List<GroupWithRoleRow>

    @Select(
        """
        SELECT u.id AS user_id, u.name AS name, u.profile_img AS profile_img, ug.role AS role, ug.created_at AS joined_at
        FROM user_groups ug
        JOIN users u ON u.id = ug.user_id
        WHERE ug.group_id = #{groupId}
        ORDER BY ug.created_at ASC
        """
    )
    fun findMembers(groupId: Long): List<GroupMemberRow>
}
