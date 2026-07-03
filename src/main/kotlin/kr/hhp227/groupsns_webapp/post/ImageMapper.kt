package kr.hhp227.groupsns_webapp.post

import org.apache.ibatis.annotations.Delete
import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Options
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select

@Mapper
interface ImageMapper {
    @Insert("INSERT INTO images(post_id, user_id, image) VALUES(#{postId}, #{userId}, #{image})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    fun insert(record: NewImageRecord): Int

    @Select("SELECT id, post_id, image FROM images WHERE post_id = #{postId} ORDER BY id ASC")
    fun findByPostId(postId: Long): List<Image>

    // 목록(피드) 조회 시 게시글마다 따로 조회하는 N+1을 피하기 위해 postId 목록을 한 번에 묶어서 조회한다.
    @Select(
        """
        <script>
        SELECT id, post_id, image FROM images
        WHERE post_id IN
        <foreach item="postId" collection="postIds" open="(" separator="," close=")">
            #{postId}
        </foreach>
        ORDER BY id ASC
        </script>
        """
    )
    fun findByPostIds(@Param("postIds") postIds: List<Long>): List<Image>

    @Delete("DELETE FROM images WHERE post_id = #{postId}")
    fun deleteByPostId(postId: Long): Int
}
