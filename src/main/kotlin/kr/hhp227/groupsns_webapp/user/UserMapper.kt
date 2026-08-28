package kr.hhp227.groupsns_webapp.user

import org.apache.ibatis.annotations.Delete
import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Options
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.Update

@Mapper
interface UserMapper {
    @Select(
        """
        SELECT id, name, email, password_hash, status, role, profile_img, bio, status_message, created_at, deleted_at
        FROM users
        WHERE email = #{email} AND deleted_at IS NULL
        """
    )
    fun findByEmail(email: String): User?

    @Select(
        """
        SELECT id, name, email, password_hash, status, role, profile_img, bio, status_message, created_at, deleted_at
        FROM users
        WHERE id = #{id} AND deleted_at IS NULL
        """
    )
    fun findById(id: Long): User?

    @Select("SELECT EXISTS(SELECT 1 FROM users WHERE email = #{email})")
    fun existsByEmail(email: String): Boolean

    @Insert("INSERT INTO users(name, email, password_hash) VALUES(#{name}, #{email}, #{passwordHash})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    fun insert(record: NewUserRecord): Int

    @Update(
        """
        UPDATE users
        SET name = #{name}, profile_img = #{profileImg}, bio = #{bio}, status_message = #{statusMessage}
        WHERE id = #{id} AND deleted_at IS NULL
        """
    )
    fun updateProfile(update: UserProfileUpdate): Int

    @Update("UPDATE users SET password_hash = #{passwordHash} WHERE id = #{id} AND deleted_at IS NULL")
    fun updatePassword(@Param("id") id: Long, @Param("passwordHash") passwordHash: String): Int

    // 탈퇴 익명화(설계 §2) — 콘텐츠 조인들이 이 name을 그대로 표시하고, email 대체값이 UNIQUE 충돌을 푼다(재가입 허용)
    @Update(
        """
        UPDATE users SET name = '탈퇴한 사용자', email = 'deleted-' || id || '@deleted.local',
            password_hash = NULL, profile_img = NULL, bio = NULL, status_message = NULL, deleted_at = now()
        WHERE id = #{userId} AND deleted_at IS NULL
        """
    )
    fun anonymize(@Param("userId") userId: Long): Int

    // OAuth 미구현이지만 테이블·행이 존재할 수 있어 개인정보 연결을 함께 끊는다(설계 §2)
    @Delete("DELETE FROM user_oauth_accounts WHERE user_id = #{userId}")
    fun deleteOauthAccounts(@Param("userId") userId: Long): Int
}
