package com.flowboard.list;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=mysecretkeymysecretkeymysecretkey",
    "jwt.expiration=86400000",
    "auth.service.url=http://localhost:8081",
    "board.service.url=http://localhost:8083"
})
class ListServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
