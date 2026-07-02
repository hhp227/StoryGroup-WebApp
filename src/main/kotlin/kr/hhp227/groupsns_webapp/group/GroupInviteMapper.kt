package kr.hhp227.groupsns_webapp.group

import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Options
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.Update

@Mapper
interface GroupInviteMapper {
    @Insert(
        """
        INSERT INTO group_invites(group_id, code, created_by, max_uses, expires_at)
        VALUES(#{groupId}, #{code}, #{createdBy}, #{maxUses}, #{expiresAt})
        """
    )
    @Options(useGeneratedKeys = true, keyProperty = "id")
    fun insert(record: NewGroupInviteRecord): Int

    @Select(
        """
        SELECT id, group_id, code, created_by, max_uses, used_count, expires_at, created_at
        FROM group_invites
        WHERE code = #{code}
        """
    )
    fun findByCode(code: String): GroupInvite?

    @Update("UPDATE group_invites SET used_count = used_count + 1 WHERE id = #{id}")
    fun incrementUsedCount(id: Long): Int
}
