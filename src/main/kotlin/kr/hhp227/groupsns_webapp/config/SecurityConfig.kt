package kr.hhp227.groupsns_webapp.config

import kr.hhp227.groupsns_webapp.security.JwtAuthenticationFilter
import kr.hhp227.groupsns_webapp.security.JwtTokenProvider
import kr.hhp227.groupsns_webapp.user.UserMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class SecurityConfig(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userMapper: UserMapper,
    @Value("\${app.cors.allowed-origins}") private val allowedOrigins: String
) : WebSecurityConfigurerAdapter() {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    override fun configure(http: HttpSecurity) {
        http
            .csrf().disable()
            .cors().configurationSource(corsConfigurationSource())
            .and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
            // /ws는 WebSocket handshake — 브라우저 WS API가 Authorization 헤더를 못 실어서
            // 여기선 열어두고, 실제 인증은 STOMP CONNECT 프레임에서 한다(StompAuthChannelInterceptor).
            .antMatchers("/", "/api/auth/**", "/ws/**").permitAll()
            .anyRequest().authenticated()
            .and()
            .addFilterBefore(
                JwtAuthenticationFilter(jwtTokenProvider, userMapper),
                UsernamePasswordAuthenticationFilter::class.java
            )
    }

    private fun corsConfigurationSource(): CorsConfigurationSource {
        // apply{} 블록 안에서 "allowedOrigins"를 직접 쓰면 CorsConfiguration 자체의
        // allowedOrigins 프로퍼티(List<String>?)에 가려져 생성자의 String 프로퍼티가 아니라
        // 그쪽으로 잘못 resolve된다(컴파일 에러의 원인이었음). 그래서 apply 밖에서 미리 split한다.
        val origins = allowedOrigins.split(",").map { it.trim() }
        val configuration = CorsConfiguration().apply {
            allowedOriginPatterns = origins
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }
}
