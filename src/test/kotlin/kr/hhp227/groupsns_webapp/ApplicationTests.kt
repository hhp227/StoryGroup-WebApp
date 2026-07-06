package kr.hhp227.groupsns_webapp

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    properties = [
        "spring.flyway.enabled=false",
        "spring.datasource.hikari.initialization-fail-timeout=0"
    ]
)
class ApplicationTests {
    @Test
    fun contextLoads() {
    }
}
