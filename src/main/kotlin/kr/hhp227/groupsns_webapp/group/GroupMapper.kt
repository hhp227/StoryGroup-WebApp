package kr.hhp227.groupsns_webapp.group

import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Options
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.Update

@Mapper
interface GroupMapper {
    @Select(
        """
        SELECT id, author_id, name, image, description, join_type, created_at, deleted_at
        FROM groups
        WHERE id = #{id} AND deleted_at IS NULL
        """
    )
    fun findById(id: Long): Group?

    @Insert(
        """
        INSERT INTO groups(author_id, name, image, description, join_type)
        VALUES(#{authorId}, #{name}, #{image}, #{description}, #{joinType})
        """
    )
    @Options(useGeneratedKeys = true, keyProperty = "id")
    fun insert(record: NewGroupRecord): Int

    @Update(
        """
        UPDATE groups
        SET name = #{name}, image = #{image}, description = #{description}
        WHERE id = #{id} AND deleted_at IS NULL
        """
    )
    fun update(update: GroupUpdate): Int

    @Update("UPDATE groups SET deleted_at = now() WHERE id = #{id} AND deleted_at IS NULL")
    fun softDelete(id: Long): Int
}
