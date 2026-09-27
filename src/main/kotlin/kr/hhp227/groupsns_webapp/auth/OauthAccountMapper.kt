package kr.hhp227.groupsns_webapp.auth

import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select

object OauthProvider {
    const val GOOGLE = "GOOGLE"
    const val APPLE = "APPLE"
}

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
}
