package kr.hhp227.groupsns_webapp.user

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
}
