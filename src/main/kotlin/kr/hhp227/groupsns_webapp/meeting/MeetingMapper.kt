package kr.hhp227.groupsns_webapp.meeting

import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Options
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.Update

@Mapper
interface MeetingMapper {
    @Select("SELECT id, group_id, host_id, started_at, ended_at FROM meetings WHERE id = #{id}")
    fun findById(id: Long): Meeting?

    @Insert("INSERT INTO meetings(group_id, host_id) VALUES(#{groupId}, #{hostId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    fun insert(record: NewMeetingRecord): Int

    @Update("UPDATE meetings SET ended_at = now() WHERE id = #{id} AND ended_at IS NULL")
    fun end(id: Long): Int

    @Select(
        """
        SELECT id, group_id, host_id, started_at, ended_at
        FROM meetings
        WHERE group_id = #{groupId}
        ORDER BY started_at DESC
        LIMIT #{limit} OFFSET #{offset}
        """
    )
    fun findByGroup(@Param("groupId") groupId: Long, @Param("limit") limit: Int, @Param("offset") offset: Int): List<Meeting>
}
