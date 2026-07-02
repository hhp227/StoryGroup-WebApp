package kr.hhp227.groupsns_webapp.auth

import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Param

@Mapper
interface LoginHistoryMapper {
    @Insert(
        """
        INSERT INTO login_history(user_id, ip_address, user_agent, success)
        VALUES(#{userId}, #{ipAddress}, #{userAgent}, #{success})
        """
    )
    fun insert(
        @Param("userId") userId: Long,
        @Param("ipAddress") ipAddress: String?,
        @Param("userAgent") userAgent: String?,
        @Param("success") success: Boolean
    ): Int
}
