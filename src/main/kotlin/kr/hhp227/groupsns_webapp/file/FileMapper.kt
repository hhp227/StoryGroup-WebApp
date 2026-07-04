package kr.hhp227.groupsns_webapp.file

import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Options
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.Update

@Mapper
interface FileMapper {
    @Select("SELECT id, group_id, user_id, name, url, size, content_type, created_at, deleted_at FROM files WHERE id = #{id} AND deleted_at IS NULL")
    fun findById(id: Long): GroupFile?

    @Insert(
        """
        INSERT INTO files(group_id, user_id, name, url, size, content_type)
        VALUES(#{groupId}, #{userId}, #{name}, #{url}, #{size}, #{contentType})
        """
    )
    @Options(useGeneratedKeys = true, keyProperty = "id")
    fun insert(record: NewFileRecord): Int

    @Update("UPDATE files SET deleted_at = now() WHERE id = #{id} AND deleted_at IS NULL")
    fun softDelete(id: Long): Int

    @Select(
        """
        SELECT f.id, f.group_id, f.user_id, u.name AS author_name, u.profile_img AS author_profile_img,
               f.name, f.url, f.size, f.content_type, f.created_at
        FROM files f
        JOIN users u ON u.id = f.user_id
        WHERE f.id = #{id} AND f.group_id = #{groupId} AND f.deleted_at IS NULL
        """
    )
    fun findFeedRowById(@Param("id") id: Long, @Param("groupId") groupId: Long): FileFeedRow?

    @Select(
        """
        SELECT f.id, f.group_id, f.user_id, u.name AS author_name, u.profile_img AS author_profile_img,
               f.name, f.url, f.size, f.content_type, f.created_at
        FROM files f
        JOIN users u ON u.id = f.user_id
        WHERE f.group_id = #{groupId} AND f.deleted_at IS NULL
        ORDER BY f.created_at DESC
        LIMIT #{limit} OFFSET #{offset}
        """
    )
    fun findFeedByGroup(@Param("groupId") groupId: Long, @Param("limit") limit: Int, @Param("offset") offset: Int): List<FileFeedRow>
}
