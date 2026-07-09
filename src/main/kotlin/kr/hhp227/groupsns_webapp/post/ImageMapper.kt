package kr.hhp227.groupsns_webapp.post

import org.apache.ibatis.annotations.Delete
import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Options
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select

@Mapper
interface ImageMapper {
    @Insert("INSERT INTO images(post_id, user_id, image, media_type) VALUES(#{postId}, #{userId}, #{image}, #{mediaType})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    fun insert(record: NewImageRecord): Int

    @Select("SELECT id, post_id, image, media_type FROM images WHERE post_id = #{postId} ORDER BY id ASC")
    fun findByPostId(postId: Long): List<Image>

    // 목록(피드) 조회 시 게시글마다 따로 조회하는 N+1을 피하기 위해 postId 목록을 한 번에 묶어서 조회한다.
    @Select(
        """
        <script>
        SELECT id, post_id, image, media_type FROM images
        WHERE post_id IN
        <foreach item="postId" collection="postIds" open="(" separator="," close=")">
            #{postId}
        </foreach>
        ORDER BY id ASC
        </script>
        """
    )
    fun findByPostIds(@Param("postIds") postIds: List<Long>): List<Image>

    // 게시글 수정의 교체 의미론이 이미지/동영상 목록별로 독립이라 media_type 단위로 지운다.
    @Delete("DELETE FROM images WHERE post_id = #{postId} AND media_type = #{mediaType}")
    fun deleteByPostIdAndType(@Param("postId") postId: Long, @Param("mediaType") mediaType: String): Int

    // 그룹 앨범(파생 뷰): 그룹 게시글에 첨부된 이미지를 최신 게시글 순으로 모아 본다.
    // 정렬을 게시글 기준으로 잡아 같은 게시글의 사진들이 갤러리에서 흩어지지 않게 한다.
    // 동영상 첨부는 앨범(사진 갤러리)에서 제외한다.
    @Select(
        """
        SELECT i.id, i.post_id, i.image, p.user_id, u.name AS author_name, p.created_at
        FROM images i
        JOIN posts p ON p.id = i.post_id
        JOIN users u ON u.id = p.user_id
        WHERE p.group_id = #{groupId} AND p.deleted_at IS NULL AND i.media_type = 'image'
        ORDER BY p.created_at DESC, i.post_id DESC, i.id ASC
        LIMIT #{limit} OFFSET #{offset}
        """
    )
    fun findGroupPhotos(@Param("groupId") groupId: Long, @Param("limit") limit: Int, @Param("offset") offset: Int): List<GroupPhotoRow>

    @Select(
        """
        SELECT COUNT(*)
        FROM images i
        JOIN posts p ON p.id = i.post_id
        WHERE p.group_id = #{groupId} AND p.deleted_at IS NULL AND i.media_type = 'image'
        """
    )
    fun countGroupPhotos(groupId: Long): Long
}
