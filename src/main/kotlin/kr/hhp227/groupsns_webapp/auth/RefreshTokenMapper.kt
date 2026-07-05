package kr.hhp227.groupsns_webapp.auth

import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Options
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.Update

@Mapper
interface RefreshTokenMapper {
    @Insert(
        """
        INSERT INTO refresh_tokens(user_id, token_hash, device_info, expires_at)
        VALUES(#{userId}, #{tokenHash}, #{deviceInfo}, #{expiresAt})
        """
    )
    @Options(useGeneratedKeys = true, keyProperty = "id")
    fun insert(record: NewRefreshTokenRecord): Int

    @Select(
        """
        SELECT id, user_id, token_hash, device_info, expires_at, revoked_at, created_at
        FROM refresh_tokens
        WHERE token_hash = #{tokenHash}
        """
    )
    fun findByTokenHash(tokenHash: String): RefreshToken?

    @Update("UPDATE refresh_tokens SET revoked_at = now() WHERE id = #{id} AND revoked_at IS NULL")
    fun revoke(id: Long): Int

    // 비밀번호 변경 시 모든 기기의 세션을 끊기 위한 일괄 무효화
    @Update("UPDATE refresh_tokens SET revoked_at = now() WHERE user_id = #{userId} AND revoked_at IS NULL")
    fun revokeAllForUser(userId: Long): Int
}
