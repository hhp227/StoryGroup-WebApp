package kr.hhp227.groupsns_webapp.push

import org.apache.ibatis.annotations.Delete
import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select

@Mapper
interface PushTokenMapper {
    // 같은 기기의 계정 전환: token 충돌 시 소유자를 새 계정으로 갈아탄다(설계 §1)
    @Insert(
        """
        INSERT INTO user_push_tokens(user_id, platform, token)
        VALUES(#{userId}, #{platform}, #{token})
        ON CONFLICT (token) DO UPDATE SET user_id = EXCLUDED.user_id, platform = EXCLUDED.platform, updated_at = now()
        """
    )
    fun upsert(@Param("userId") userId: Long, @Param("platform") platform: String, @Param("token") token: String): Int

    // 소유자 무관 삭제 — 로그아웃하는 기기의 토큰은 어떤 계정 소유든 지워져야 한다
    @Delete("DELETE FROM user_push_tokens WHERE token = #{token}")
    fun deleteByToken(@Param("token") token: String): Int

    @Select("SELECT token FROM user_push_tokens WHERE user_id = #{userId}")
    fun findTokensByUser(@Param("userId") userId: Long): List<String>

    // 탈퇴 시 전 기기 푸시 중단(설계 §2)
    @Delete("DELETE FROM user_push_tokens WHERE user_id = #{userId}")
    fun deleteAllForUser(@Param("userId") userId: Long): Int
}
