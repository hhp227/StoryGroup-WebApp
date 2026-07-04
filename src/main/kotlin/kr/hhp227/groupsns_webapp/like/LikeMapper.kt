package kr.hhp227.groupsns_webapp.like

import org.apache.ibatis.annotations.Delete
import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select

@Mapper
interface LikeMapper {
    @Select("SELECT EXISTS(SELECT 1 FROM post_likes WHERE user_id = #{userId} AND post_id = #{postId})")
    fun exists(@Param("userId") userId: Long, @Param("postId") postId: Long): Boolean

    @Insert("INSERT INTO post_likes(user_id, post_id) VALUES(#{userId}, #{postId})")
    fun insert(record: NewLikeRecord): Int

    @Delete("DELETE FROM post_likes WHERE user_id = #{userId} AND post_id = #{postId}")
    fun delete(@Param("userId") userId: Long, @Param("postId") postId: Long): Int

    @Select(
        """
        SELECT l.user_id, u.name AS author_name, u.profile_img AS author_profile_img, l.created_at
        FROM post_likes l
        JOIN users u ON u.id = l.user_id
        WHERE l.post_id = #{postId}
        ORDER BY l.created_at DESC
        LIMIT #{limit} OFFSET #{offset}
        """
    )
    fun findFeedByPost(@Param("postId") postId: Long, @Param("limit") limit: Int, @Param("offset") offset: Int): List<LikeFeedRow>
}
