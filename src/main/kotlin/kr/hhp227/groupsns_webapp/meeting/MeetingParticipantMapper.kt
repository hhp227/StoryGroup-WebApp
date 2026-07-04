package kr.hhp227.groupsns_webapp.meeting

import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.Update

@Mapper
interface MeetingParticipantMapper {
    @Select("SELECT EXISTS(SELECT 1 FROM meeting_participants WHERE meeting_id = #{meetingId} AND user_id = #{userId} AND left_at IS NULL)")
    fun hasActiveParticipant(@Param("meetingId") meetingId: Long, @Param("userId") userId: Long): Boolean

    @Insert("INSERT INTO meeting_participants(meeting_id, user_id) VALUES(#{meetingId}, #{userId})")
    fun insert(record: NewParticipantRecord): Int

    @Update("UPDATE meeting_participants SET left_at = now() WHERE meeting_id = #{meetingId} AND user_id = #{userId} AND left_at IS NULL")
    fun leave(@Param("meetingId") meetingId: Long, @Param("userId") userId: Long): Int

    // 호스트가 회의를 종료할 때 남아 있는 모든 참가자를 일괄 퇴장 처리한다.
    @Update("UPDATE meeting_participants SET left_at = now() WHERE meeting_id = #{meetingId} AND left_at IS NULL")
    fun leaveAllActive(meetingId: Long): Int

    @Select(
        """
        SELECT mp.user_id, u.name AS author_name, u.profile_img AS author_profile_img, mp.joined_at, mp.left_at
        FROM meeting_participants mp
        JOIN users u ON u.id = mp.user_id
        WHERE mp.meeting_id = #{meetingId}
        ORDER BY mp.joined_at ASC
        """
    )
    fun findFeedByMeeting(meetingId: Long): List<ParticipantFeedRow>
}
