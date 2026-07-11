package kr.hhp227.groupsns_webapp.event

import org.apache.ibatis.annotations.Delete
import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Options
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.Update
import java.time.OffsetDateTime

// EventRow 공용 SELECT: 작성자 조인 + RSVP 상태별 집계 + 조회자 본인 RSVP 서브쿼리.
private const val EVENT_ROW_SELECT = """
    SELECT e.id, e.group_id, e.user_id, u.name AS author_name, u.profile_img AS author_profile_img,
           e.title, e.description, e.location, e.starts_at, e.ends_at, e.created_at,
           COUNT(r.id) FILTER (WHERE r.status = 'GOING') AS going_count,
           COUNT(r.id) FILTER (WHERE r.status = 'MAYBE') AS maybe_count,
           COUNT(r.id) FILTER (WHERE r.status = 'NOT_GOING') AS not_going_count,
           (SELECT mr.status FROM event_rsvps mr WHERE mr.event_id = e.id AND mr.user_id = #{viewerId}) AS my_rsvp
    FROM events e
    JOIN users u ON u.id = e.user_id
    LEFT JOIN event_rsvps r ON r.event_id = e.id
"""

private const val EVENT_ROW_GROUP_BY = "GROUP BY e.id, u.name, u.profile_img"

@Mapper
interface EventMapper {
    @Insert(
        """
        INSERT INTO events(group_id, user_id, title, description, location, starts_at, ends_at)
        VALUES(#{groupId}, #{userId}, #{title}, #{description}, #{location}, #{startsAt}, #{endsAt})
        """
    )
    @Options(useGeneratedKeys = true, keyProperty = "id")
    fun insert(record: NewEventRecord): Int

    @Update(
        """
        UPDATE events
        SET title = #{title}, description = #{description}, location = #{location},
            starts_at = #{startsAt}, ends_at = #{endsAt}
        WHERE id = #{id} AND deleted_at IS NULL
        """
    )
    fun update(update: EventUpdate): Int

    @Update("UPDATE events SET deleted_at = now() WHERE id = #{id} AND deleted_at IS NULL")
    fun softDelete(id: Long): Int

    @Select(
        """
        $EVENT_ROW_SELECT
        WHERE e.id = #{id} AND e.deleted_at IS NULL
        $EVENT_ROW_GROUP_BY
        """
    )
    fun findById(@Param("id") id: Long, @Param("viewerId") viewerId: Long): EventRow?

    // 캘린더 범위 조회. viewerId가 차단한 작성자의 일정은 숨긴다(차단 숨김은 쿼리 레벨 - 앱 DB 롤은 RLS 우회).
    @Select(
        """
        $EVENT_ROW_SELECT
        WHERE e.group_id = #{groupId} AND e.deleted_at IS NULL
          AND e.starts_at >= #{from} AND e.starts_at < #{to}
          AND NOT EXISTS (SELECT 1 FROM user_blocks ub WHERE ub.blocker_id = #{viewerId} AND ub.blocked_id = e.user_id)
        $EVENT_ROW_GROUP_BY
        ORDER BY e.starts_at ASC, e.id ASC
        """
    )
    fun findByGroupBetween(
        @Param("groupId") groupId: Long,
        @Param("from") from: OffsetDateTime,
        @Param("to") to: OffsetDateTime,
        @Param("viewerId") viewerId: Long
    ): List<EventRow>

    // 사이드바 "다가오는 일정": 아직 시작 전인 일정을 가까운 순으로.
    @Select(
        """
        $EVENT_ROW_SELECT
        WHERE e.group_id = #{groupId} AND e.deleted_at IS NULL
          AND e.starts_at >= now()
          AND NOT EXISTS (SELECT 1 FROM user_blocks ub WHERE ub.blocker_id = #{viewerId} AND ub.blocked_id = e.user_id)
        $EVENT_ROW_GROUP_BY
        ORDER BY e.starts_at ASC, e.id ASC
        LIMIT #{limit}
        """
    )
    fun findUpcomingByGroup(
        @Param("groupId") groupId: Long,
        @Param("viewerId") viewerId: Long,
        @Param("limit") limit: Int
    ): List<EventRow>
}

@Mapper
interface EventRsvpMapper {
    @Insert(
        """
        INSERT INTO event_rsvps(event_id, user_id, status)
        VALUES(#{eventId}, #{userId}, #{status})
        ON CONFLICT (event_id, user_id) DO UPDATE SET status = EXCLUDED.status, updated_at = now()
        """
    )
    fun upsert(@Param("eventId") eventId: Long, @Param("userId") userId: Long, @Param("status") status: String): Int

    @Delete("DELETE FROM event_rsvps WHERE event_id = #{eventId} AND user_id = #{userId}")
    fun delete(@Param("eventId") eventId: Long, @Param("userId") userId: Long): Int

    // 참석/미정/불참 순 → 같은 상태끼리는 응답한 순.
    @Select(
        """
        SELECT r.user_id, u.name, u.profile_img, r.status
        FROM event_rsvps r
        JOIN users u ON u.id = r.user_id
        WHERE r.event_id = #{eventId}
        ORDER BY CASE r.status WHEN 'GOING' THEN 0 WHEN 'MAYBE' THEN 1 ELSE 2 END, r.updated_at ASC
        """
    )
    fun findByEvent(@Param("eventId") eventId: Long): List<EventRsvpRow>
}
