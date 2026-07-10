package kr.hhp227.groupsns_webapp.comment

import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Options
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.Update

@Mapper
interface CommentMapper {
    @Insert(
        """
        INSERT INTO replys(user_id, parent_reply_id, reply)
        VALUES(#{userId}, #{parentReplyId}, #{reply})
        """
    )
    @Options(useGeneratedKeys = true, keyProperty = "id")
    fun insert(record: NewReplyRecord): Int

    @Update("UPDATE replys SET reply = #{reply} WHERE id = #{id} AND deleted_at IS NULL")
    fun update(update: CommentUpdate): Int

    @Update("UPDATE replys SET deleted_at = now() WHERE id = #{id} AND deleted_at IS NULL")
    fun softDelete(id: Long): Int

    @Select(
        """
        SELECT r.id, ur.post_id, r.user_id, u.name AS author_name, u.profile_img AS author_profile_img,
               r.parent_reply_id, r.reply, r.created_at
        FROM replys r
        JOIN user_replys ur ON ur.reply_id = r.id
        JOIN users u ON u.id = r.user_id
        WHERE r.id = #{id} AND ur.post_id = #{postId} AND r.deleted_at IS NULL
        """
    )
    fun findFeedRowById(@Param("id") id: Long, @Param("postId") postId: Long): CommentFeedRow?

    // viewerId가 차단한 작성자의 댓글은 숨긴다(차단 숨김은 쿼리 레벨 - 앱 DB 롤은 RLS 우회).
    @Select(
        """
        SELECT r.id, ur.post_id, r.user_id, u.name AS author_name, u.profile_img AS author_profile_img,
               r.parent_reply_id, r.reply, r.created_at
        FROM replys r
        JOIN user_replys ur ON ur.reply_id = r.id
        JOIN users u ON u.id = r.user_id
        WHERE ur.post_id = #{postId} AND r.deleted_at IS NULL
          AND NOT EXISTS (SELECT 1 FROM user_blocks ub WHERE ub.blocker_id = #{viewerId} AND ub.blocked_id = r.user_id)
        ORDER BY r.created_at ASC
        LIMIT #{limit} OFFSET #{offset}
        """
    )
    fun findFeedByPost(
        @Param("postId") postId: Long,
        @Param("viewerId") viewerId: Long,
        @Param("limit") limit: Int,
        @Param("offset") offset: Int
    ): List<CommentFeedRow>
}
