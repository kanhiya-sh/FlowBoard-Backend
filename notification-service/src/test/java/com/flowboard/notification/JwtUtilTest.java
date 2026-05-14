package com.flowboard.notification;

import com.flowboard.notification.util.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET = "mysecretkeymysecretkeymysecretkeymysecretkey";
    private JwtUtil jwtUtil;
    private Key key;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        key = Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    private String generateToken(String subject, long expiryOffsetMs) {
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiryOffsetMs))
                .signWith(key)
                .compact();
    }

    @Test
    void extractEmail_returnsSubjectClaim() {
        String token = generateToken("alice@test.com", 60_000);
        assertEquals("alice@test.com", jwtUtil.extractEmail(token));
    }

    @Test
    void validateToken_returnsTrueForValidNonExpiredToken() {
        String token = generateToken("bob@test.com", 60_000);
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_returnsFalseForExpiredToken() {
        String token = generateToken("carol@test.com", -60_000);
        assertFalse(jwtUtil.validateToken(token));
    }

    @Test
    void validateToken_returnsFalseForMalformedToken() {
        assertFalse(jwtUtil.validateToken("not-a-valid-jwt"));
    }

    @Test
    void validateToken_returnsFalseForEmptyToken() {
        assertFalse(jwtUtil.validateToken(""));
    }

    @Test
    void validateToken_returnsFalseForTokenSignedWithDifferentKey() {
        String wrongSecret = "differentsecretdifferentsecretdifferentsecret";
        String token = Jwts.builder()
                .setSubject("eve@test.com")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(wrongSecret.getBytes()))
                .compact();
        assertFalse(jwtUtil.validateToken(token));
    }
}
