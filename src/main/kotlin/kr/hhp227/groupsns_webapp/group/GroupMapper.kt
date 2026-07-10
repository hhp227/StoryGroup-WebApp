package kr.hhp227.groupsns_webapp.group

import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Options
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.Update

@Mapper
interface GroupMapper {
    @Select(
        """
        SELECT id, author_id, name, image, description, join_type, created_at, deleted_at, is_lounge
        FROM groups
        WHERE id = #{id} AND deleted_at IS NULL
        """
    )
    fun findById(id: Long): Group?

    // AuthService가 신규 가입자를 자동 가입시키기 위해 조회. 라운지가 아직 없으면(마이그레이션 전) null.
    @Select(
        """
        SELECT id, author_id, name, image, description, join_type, created_at, deleted_at, is_lounge
        FROM groups
        WHERE is_lounge = true AND deleted_at IS NULL
        """
    )
    fun findLounge(): Group?

    @Insert(
        """
        INSERT INTO groups(author_id, name, image, description, join_type)
        VALUES(#{authorId}, #{name}, #{image}, #{description}, #{joinType})
        """
    )
    @Options(useGeneratedKeys = true, keyProperty = "id")
    fun insert(record: NewGroupRecord): Int

    @Update(
        """
        UPDATE groups
        SET name = #{name}, image = #{image}, description = #{description},
            join_type = COALESCE(#{joinType}, join_type)
        WHERE id = #{id} AND deleted_at IS NULL
        """
    )
    fun update(update: GroupUpdate): Int

    // 그룹 탐색: 라운지/삭제 그룹 제외 전체 노출(레거시 동작). 멤버 수는 user_groups RLS(멤버만
    // SELECT 가능)를 우회하는 SECURITY DEFINER 함수 group_member_count()로 계산하고,
    // "나와의 관계"는 내 행만 LEFT JOIN해 확인한다(내 멤버십/신청 행은 RLS 정책상 항상 보인다).
    @Select(
        """
        SELECT g.id, g.name, g.image, g.description, g.join_type, g.created_at,
               group_member_count(g.id) AS member_count,
               (ug.user_id IS NOT NULL) AS is_member,
               (jr.id IS NOT NULL) AS is_pending
        FROM groups g
        LEFT JOIN user_groups ug ON ug.group_id = g.id AND ug.user_id = #{userId}
        LEFT JOIN group_join_requests jr ON jr.group_id = g.id AND jr.user_id = #{userId}
        WHERE g.deleted_at IS NULL AND g.is_lounge = false
          AND (#{query} = '' OR g.name ILIKE '%' || #{query} || '%' OR g.description ILIKE '%' || #{query} || '%')
        ORDER BY g.created_at DESC
        LIMIT #{size} OFFSET #{offset}
        """
    )
    fun findDiscoverGroups(
        @Param("userId") userId: Long,
        @Param("query") query: String,
        @Param("size") size: Int,
        @Param("offset") offset: Int
    ): List<DiscoverGroupRow>

    @Update("UPDATE groups SET deleted_at = now() WHERE id = #{id} AND deleted_at IS NULL")
    fun softDelete(id: Long): Int
}
