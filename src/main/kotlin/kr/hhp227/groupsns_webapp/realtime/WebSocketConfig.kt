package kr.hhp227.groupsns_webapp.realtime

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val stompAuthChannelInterceptor: StompAuthChannelInterceptor,
    @Value("\${app.cors.allowed-origins}") private val allowedOrigins: String
) : WebSocketMessageBrokerConfigurer {

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        // handshake는 permitAll이지만 Origin은 REST CORS와 같은 목록으로 제한한다.
        val origins = allowedOrigins.split(",").map { it.trim() }
        registry.addEndpoint("/ws").setAllowedOriginPatterns(*origins.toTypedArray())
    }

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        // 인메모리 SimpleBroker — 인스턴스 간 팬아웃이 안 되므로 Cloud Run max-instances=1이 전제
        // (설계 문서 D2, 확장 시 ChatEventBroadcaster를 릴레이 구현으로 교체).
        registry.enableSimpleBroker("/topic")
            .setHeartbeatValue(longArrayOf(10_000, 10_000))
            .setTaskScheduler(wsHeartbeatTaskScheduler())
        // 이번 마일스톤엔 @MessageMapping이 없지만 Typing 등 후속 기능(D7)이 쓸 prefix.
        registry.setApplicationDestinationPrefixes("/app")
    }

    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(stompAuthChannelInterceptor)
    }

    // 이름을 messageBrokerTaskScheduler로 지으면 Spring의 DelegatingWebSocketMessageBrokerConfiguration이
    // 등록하는 동명 빈과 충돌해 기동이 실패한다(BeanDefinitionOverrideException, 2026-07-06 실제 발생).
    @Bean
    fun wsHeartbeatTaskScheduler(): ThreadPoolTaskScheduler = ThreadPoolTaskScheduler().apply {
        poolSize = 1
        setThreadNamePrefix("ws-heartbeat-")
        initialize()
    }
}
