package kr.hhp227.groupsns_webapp.report

import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.Update

@Mapper
interface UserReportMapper {
    @Insert(
        """
        INSERT INTO user_reports(reporter_id, reported_id, reason)
        VALUES(#{reporterId}, #{reportedId}, #{reason})
        """
    )
    fun insert(@Param("reporterId") reporterId: Long, @Param("reportedId") reportedId: Long, @Param("reason") reason: String?): Int

    @Select(
        """
        SELECT EXISTS (
            SELECT 1 FROM user_reports
            WHERE reporter_id = #{reporterId} AND reported_id = #{reportedId} AND status = 'PENDING'
        )
        """
    )
    fun existsPending(@Param("reporterId") reporterId: Long, @Param("reportedId") reportedId: Long): Boolean

    // 운영자 신고 관리 목록. 탈퇴한 사용자가 낀 신고도 그대로 보여준다(처리 기록이므로).
    // reported_total_count = 피신고자의 전체 누적 신고 수(상태 무관) - 상습 피신고자 식별용.
    @Select(
        """
        <script>
        SELECT ur.id, ur.reporter_id, rr.name AS reporter_name,
               ur.reported_id, rd.name AS reported_name, rd.profile_img AS reported_profile_img,
               ur.reason, ur.status, ur.created_at, ur.processed_at,
               (SELECT COUNT(*) FROM user_reports c WHERE c.reported_id = ur.reported_id) AS reported_total_count
        FROM user_reports ur
        JOIN users rr ON rr.id = ur.reporter_id
        JOIN users rd ON rd.id = ur.reported_id
        <where>
            <if test="status != null">ur.status = #{status}</if>
        </where>
        ORDER BY ur.created_at DESC
        LIMIT #{size} OFFSET #{offset}
        </script>
        """
    )
    fun findAll(
        @Param("status") status: ReportStatus?,
        @Param("size") size: Int,
        @Param("offset") offset: Int
    ): List<UserReportRow>

    @Select(
        """
        SELECT ur.id, ur.reporter_id, rr.name AS reporter_name,
               ur.reported_id, rd.name AS reported_name, rd.profile_img AS reported_profile_img,
               ur.reason, ur.status, ur.created_at, ur.processed_at,
               (SELECT COUNT(*) FROM user_reports c WHERE c.reported_id = ur.reported_id) AS reported_total_count
        FROM user_reports ur
        JOIN users rr ON rr.id = ur.reporter_id
        JOIN users rd ON rd.id = ur.reported_id
        WHERE ur.id = #{id}
        """
    )
    fun findById(id: Long): UserReportRow?

    @Update(
        """
        UPDATE user_reports
        SET status = #{status}, processed_at = now(), processed_by = #{processedBy}
        WHERE id = #{id}
        """
    )
    fun updateStatus(@Param("id") id: Long, @Param("status") status: ReportStatus, @Param("processedBy") processedBy: Long): Int
}
