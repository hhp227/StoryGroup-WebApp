package kr.hhp227.groupsns_webapp.post

import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Options
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.Update

@Mapper
interface PostMapper {
    @Select(
        """
        SELECT id, group_id, user_id, text, created_at, deleted_at
        FROM posts
        WHERE id = #{id} AND deleted_at IS NULL
        """
    )
    fun findById(id: Long): Post?

    @Insert(
        """
        INSERT INTO posts(group_id, user_id, text)
        VALUES(#{groupId}, #{userId}, #{text})
        """
    )
    @Options(useGeneratedKeys = true, keyProperty = "id")
    fun insert(record: NewPostRecord): Int

    @Update("UPDATE posts SET text = #{text} WHERE id = #{id} AND deleted_at IS NULL")
    fun update(update: PostUpdate): Int

    @Update("UPDATE posts SET deleted_at = now() WHERE id = #{id} AND deleted_at IS NULL")
    fun softDelete(id: Long): Int

    @Select(
        """
        SELECT p.id, p.group_id, p.user_id, u.name AS author_name, u.profile_img AS author_profile_img,
               p.text, p.created_at
        FROM posts p
        JOIN users u ON u.id = p.user_id
        WHERE p.id = #{id} AND p.group_id = #{groupId} AND p.deleted_at IS NULL
        """
    )
    fun findFeedRowById(@Param("id") id: Long, @Param("groupId") groupId: Long): PostFeedRow?

    @Select(
        """
        SELECT p.id, p.group_id, p.user_id, u.name AS author_name, u.profile_img AS author_profile_img,
               p.text, p.created_at
        FROM posts p
        JOIN users u ON u.id = p.user_id
        WHERE p.group_id = #{groupId} AND p.deleted_at IS NULL
        ORDER BY p.created_at DESC
        LIMIT #{limit} OFFSET #{offset}
        """
    )
    fun findFeedByGroup(@Param("groupId") groupId: Long, @Param("limit") limit: Int, @Param("offset") offset: Int): List<PostFeedRow>
}
