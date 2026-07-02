package kr.hhp227.groupsns_webapp.security

import kr.hhp227.groupsns_webapp.user.User
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class UserPrincipal(
    val id: Long,
    private val email: String
) : UserDetails {
    companion object {
        fun from(user: User): UserPrincipal = UserPrincipal(user.id, user.email)
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
