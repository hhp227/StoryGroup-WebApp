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

    // 공지 지정/해제. is_notice(공지 표시)와 is_pinned(상단 고정)는 현재 항상 함께 움직인다.
    @Update("UPDATE posts SET is_notice = #{notice}, is_pinned = #{notice} WHERE id = #{id} AND deleted_at IS NULL")
    fun setNotice(@Param("id") id: Long, @Param("notice") notice: Boolean): Int

    // viewerId가 차단한 작성자의 글은 숨긴다(단건 직접 조회도 404가 되도록 여기서 거른다).
    // 앱 DB 롤은 RLS를 우회하므로 차단 숨김은 이렇게 쿼리 레벨에서 처리한다.
    @Select(
        """
        SELECT p.id, p.group_id, p.user_id, u.name AS author_name, u.profile_img AS author_profile_img,
               p.text, p.is_notice, p.created_at
        FROM posts p
        JOIN users u ON u.id = p.user_id
        WHERE p.id = #{id} AND p.group_id = #{groupId} AND p.deleted_at IS NULL
          AND NOT EXISTS (SELECT 1 FROM user_blocks ub WHERE ub.blocker_id = #{viewerId} AND ub.blocked_id = p.user_id)
        """
    )
    fun findFeedRowById(@Param("id") id: Long, @Param("groupId") groupId: Long, @Param("viewerId") viewerId: Long): PostFeedRow?

    // 공지는 피드에서 제외한다 — 사이드바 공지 패널(/notices)이 공지의 노출 창구(상단 고정 방식 폐기).
    @Select(
        """
        SELECT p.id, p.group_id, p.user_id, u.name AS author_name, u.profile_img AS author_profile_img,
               p.text, p.is_notice, p.created_at
        FROM posts p
        JOIN users u ON u.id = p.user_id
        WHERE p.group_id = #{groupId} AND p.is_notice = false AND p.deleted_at IS NULL
          AND NOT EXISTS (SELECT 1 FROM user_blocks ub WHERE ub.blocker_id = #{viewerId} AND ub.blocked_id = p.user_id)
        ORDER BY p.created_at DESC
        LIMIT #{limit} OFFSET #{offset}
        """
    )
    fun findFeedByGroup(
        @Param("groupId") groupId: Long,
        @Param("viewerId") viewerId: Long,
        @Param("limit") limit: Int,
        @Param("offset") offset: Int
    ): List<PostFeedRow>

    // 사이드바 공지 패널(최근 N건) + 공지 전체 페이지(페이지네이션) 공용.
    @Select(
        """
        SELECT p.id, p.group_id, p.user_id, u.name AS author_name, u.profile_img AS author_profile_img,
               p.text, p.is_notice, p.created_at
        FROM posts p
        JOIN users u ON u.id = p.user_id
        WHERE p.group_id = #{groupId} AND p.is_notice = true AND p.deleted_at IS NULL
          AND NOT EXISTS (SELECT 1 FROM user_blocks ub WHERE ub.blocker_id = #{viewerId} AND ub.blocked_id = p.user_id)
        ORDER BY p.created_at DESC
        LIMIT #{limit} OFFSET #{offset}
        """
    )
    fun findNotices(
        @Param("groupId") groupId: Long,
        @Param("viewerId") viewerId: Long,
        @Param("limit") limit: Int,
        @Param("offset") offset: Int
    ): List<PostFeedRow>

    @Select(
        """
        SELECT COUNT(*) FROM posts p
        WHERE p.group_id = #{groupId} AND p.is_notice = true AND p.deleted_at IS NULL
          AND NOT EXISTS (SELECT 1 FROM user_blocks ub WHERE ub.blocker_id = #{viewerId} AND ub.blocked_id = p.user_id)
        """
    )
    fun countNotices(@Param("groupId") groupId: Long, @Param("viewerId") viewerId: Long): Long
}
