package kr.hhp227.groupsns_webapp.security

import kr.hhp227.groupsns_webapp.user.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class UserPrincipal(
    val id: Long,
    // Typing 같은 실시간 이벤트에 표시 이름을 실어 보내기 위해 보관 — 매 신호마다 DB 조회를 피한다.
    val name: String,
    private val email: String
) : UserDetails {
    companion object {
        fun from(user: User): UserPrincipal = UserPrincipal(user.id, user.name, user.email)
    }

    override fun getAuthorities(): Collection<GrantedAuthority> = listOf(SimpleGrantedAuthority("ROLE_USER"))

    // JWT 필터가 토큰 검증 후 만드는 principal이라 비밀번호 재확인이 불필요함(null 고정).
    override fun getPassword(): String? = null
    override fun getUsername(): String = email
    override fun isAccountNonExpired() = true
    override fun isAccountNonLocked() = true
    override fun isCredentialsNonExpired() = true
    override fun isEnabled() = true
}
