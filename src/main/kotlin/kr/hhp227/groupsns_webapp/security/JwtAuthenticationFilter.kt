package kr.hhp227.groupsns_webapp.security

import io.jsonwebtoken.JwtException
import kr.hhp227.groupsns_webapp.user.UserMapper
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userMapper: UserMapper
) : OncePerRequestFilter() {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        resolveToken(request)?.let { token ->
            try {
                val userId = jwtTokenProvider.getUserId(token)
                val user = userMapper.findById(userId)
                if (user != null) {
                    val principal = UserPrincipal.from(user)
                    val authentication = UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
                    authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authentication
                }
            } catch (ex: JwtException) {
                SecurityContextHolder.clearContext()
            } catch (ex: IllegalArgumentException) {
                SecurityContextHolder.clearContext()
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        val header = request.getHeader("Authorization") ?: return null
        return if (header.startsWith("Bearer ")) header.removePrefix("Bearer ") else null
    }
}
