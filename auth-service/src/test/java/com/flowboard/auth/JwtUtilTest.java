package com.flowboard.auth;

import com.flowboard.auth.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "mysecretkeymysecretkeymysecretkey");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 86400000L);
    }

    @Test
    void generateToken_notNull() {
        String token = jwtUtil.generateToken("test@example.com");
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void extractEmail_returnsCorrectEmail() {
        String token = jwtUtil.generateToken("test@example.com");
        assertEquals("test@example.com", jwtUtil.extractEmail(token));
    }

    @Test
    void validateToken_validToken_returnsTrue() {
        String token = jwtUtil.generateToken("test@example.com");
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_invalidToken_returnsFalse() {
        assertFalse(jwtUtil.validateToken("invalid.token.here"));
    }

    @Test
    void validateToken_tamperedToken_returnsFalse() {
        String token = jwtUtil.generateToken("test@example.com");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertFalse(jwtUtil.validateToken(tampered));
    }
}
