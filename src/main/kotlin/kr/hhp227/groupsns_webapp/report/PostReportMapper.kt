package kr.hhp227.groupsns_webapp.report

import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.Update

@Mapper
interface PostReportMapper {
    @Insert(
        """
        INSERT INTO post_reports(user_id, post_id, reason)
        VALUES(#{userId}, #{postId}, #{reason})
        """
    )
    fun insert(@Param("userId") userId: Long, @Param("postId") postId: Long, @Param("reason") reason: String?): Int

    @Select(
        """
        SELECT EXISTS (
            SELECT 1 FROM post_reports
            WHERE user_id = #{userId} AND post_id = #{postId} AND status = 'PENDING'
        )
        """
    )
    fun existsPending(@Param("userId") userId: Long, @Param("postId") postId: Long): Boolean

    // 그룹 신고함 목록. 신고된 게시글이 이미 삭제됐으면 조치 대상이 사라진 것이므로 목록에서 뺀다.
    @Select(
        """
        <script>
        SELECT pr.id, pr.post_id, p.text AS post_text, p.user_id AS post_author_id, pa.name AS post_author_name,
               pr.user_id AS reporter_id, ru.name AS reporter_name,
               pr.reason, pr.status, pr.created_at, pr.processed_at
        FROM post_reports pr
        JOIN posts p ON p.id = pr.post_id AND p.deleted_at IS NULL
        JOIN users pa ON pa.id = p.user_id
        JOIN users ru ON ru.id = pr.user_id
        WHERE p.group_id = #{groupId}
        <if test="status != null">AND pr.status = #{status}</if>
        ORDER BY pr.created_at DESC
        LIMIT #{size} OFFSET #{offset}
        </script>
        """
    )
    fun findByGroup(
        @Param("groupId") groupId: Long,
        @Param("status") status: ReportStatus?,
        @Param("size") size: Int,
        @Param("offset") offset: Int
    ): List<PostReportRow>

    // 단건 조회(처리 대상 검증/처리 후 응답 공용). 목록과 달리 삭제된 게시글도 조회된다 -
    // 그룹 스코프 판정에 필요한 조인이라 deleted_at 조건을 걸지 않는다.
    @Select(
        """
        SELECT pr.id, pr.post_id, p.text AS post_text, p.user_id AS post_author_id, pa.name AS post_author_name,
               pr.user_id AS reporter_id, ru.name AS reporter_name,
               pr.reason, pr.status, pr.created_at, pr.processed_at
        FROM post_reports pr
        JOIN posts p ON p.id = pr.post_id
        JOIN users pa ON pa.id = p.user_id
        JOIN users ru ON ru.id = pr.user_id
        WHERE pr.id = #{id} AND p.group_id = #{groupId}
        """
    )
    fun findByIdInGroup(@Param("id") id: Long, @Param("groupId") groupId: Long): PostReportRow?

    @Update(
        """
        UPDATE post_reports
        SET status = #{status}, processed_at = now(), processed_by = #{processedBy}
        WHERE id = #{id}
        """
    )
    fun updateStatus(@Param("id") id: Long, @Param("status") status: ReportStatus, @Param("processedBy") processedBy: Long): Int
}
