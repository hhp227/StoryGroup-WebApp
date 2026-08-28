package kr.hhp227.groupsns_webapp.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

// FCM 왕복(수백 ms)이 요청 스레드·WS 방송을 막지 않게 하는 전용 소형 풀.
// 이 리포의 첫 @EnableAsync — 다른 @Async가 생기면 executor 지정을 잊지 말 것.
@Configuration
@EnableAsync
class AsyncConfig {
    @Bean
    fun pushTaskExecutor(): ThreadPoolTaskExecutor = ThreadPoolTaskExecutor().apply {
        corePoolSize = 1
        maxPoolSize = 2
        queueCapacity = 500
        setThreadNamePrefix("push-")
        initialize()
    }
}
