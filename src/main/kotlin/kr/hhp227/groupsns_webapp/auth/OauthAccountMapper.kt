package kr.hhp227.groupsns_webapp.auth

import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.Update

object OauthProvider {
    const val GOOGLE = "GOOGLE"
    const val APPLE = "APPLE"
}

// 컬럼 순서가 생성자 순서와 같아야 한다(자동 매핑)
data class OauthToken(val refreshToken: String, val clientId: String)

// user_oauth_accounts(V1) — (provider, provider_user_id) UNIQUE. 탈퇴 시 UserMapper.deleteOauthAccounts가 지운다
@Mapper
interface OauthAccountMapper {
    @Select("SELECT user_id FROM user_oauth_accounts WHERE provider = #{provider} AND provider_user_id = #{providerUserId}")
    fun findUserId(@Param("provider") provider: String, @Param("providerUserId") providerUserId: String): Long?

    @Insert("INSERT INTO user_oauth_accounts(user_id, provider, provider_user_id) VALUES(#{userId}, #{provider}, #{providerUserId})")
    fun insert(
        @Param("userId") userId: Long,
        @Param("provider") provider: String,
        @Param("providerUserId") providerUserId: String
    ): Int

    @Update(
        "UPDATE user_oauth_accounts SET refresh_token = #{refreshToken}, client_id = #{clientId} " +
            "WHERE provider = #{provider} AND provider_user_id = #{providerUserId}"
    )
    fun updateToken(
        @Param("provider") provider: String,
        @Param("providerUserId") providerUserId: String,
        @Param("refreshToken") refreshToken: String,
        @Param("clientId") clientId: String
    ): Int

    @Select(
        "SELECT refresh_token, client_id FROM user_oauth_accounts " +
            "WHERE user_id = #{userId} AND provider = #{provider} AND refresh_token IS NOT NULL AND client_id IS NOT NULL"
    )
    fun findTokens(@Param("userId") userId: Long, @Param("provider") provider: String): List<OauthToken>
}
