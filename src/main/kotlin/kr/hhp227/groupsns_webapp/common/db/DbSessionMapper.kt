package kr.hhp227.groupsns_webapp.common.db

import org.apache.ibatis.annotations.Mapper
import org.apache.ibatis.annotations.Select

@Mapper
interface DbSessionMapper {
    // V2__row_level_security.sql의 RLS 정책이 참조하는 세션 변수를 채워준다.
    // set_config(..., is_local=true)는 SET LOCAL과 동일하게 현재 트랜잭션에서만 유효하므로,
    // @Transactional 메서드 맨 앞에서 호출해야 같은 커넥션/트랜잭션 범위에 적용된다.
    // (SET LOCAL 문 자체는 JDBC 바인드 파라미터를 못 받아 set_config() 함수 호출로 대체함)
    @Select("SELECT set_config('app.current_user_id', CAST(#{userId} AS text), true)")
    fun setCurrentUserId(userId: Long): String
}
