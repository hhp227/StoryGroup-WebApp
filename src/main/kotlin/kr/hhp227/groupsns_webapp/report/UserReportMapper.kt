package kr.hhp227.groupsns_webapp.report

import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select

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
            SELECT 1 FROM user_reports WHERE reporter_id = #{reporterId} AND reported_id = #{reportedId}
        )
        """
    )
    fun exists(@Param("reporterId") reporterId: Long, @Param("reportedId") reportedId: Long): Boolean
}
