package com.flowboard.card;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=mysecretkeymysecretkeymysecretkey",
    "jwt.expiration=86400000",
    "eureka.client.enabled=false",
    "spring.cloud.discovery.enabled=false",
    "spring.cache.type=none"
})
class CardServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
