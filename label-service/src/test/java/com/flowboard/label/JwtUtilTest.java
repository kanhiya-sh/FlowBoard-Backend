package com.flowboard.label;

import com.flowboard.label.util.JwtUtil;
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

    private String token(String subject, long offsetMs) {
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + offsetMs))
                .signWith(key)
                .compact();
    }

    @Test
    void extractEmail_returnsSubject() {
        assertEquals("alice@test.com", jwtUtil.extractEmail(token("alice@test.com", 60_000)));
    }

    @Test
    void validateToken_trueForValid() {
        assertTrue(jwtUtil.validateToken(token("a@b.com", 60_000)));
    }

    @Test
    void validateToken_falseForExpired() {
        assertFalse(jwtUtil.validateToken(token("a@b.com", -60_000)));
    }

    @Test
    void validateToken_falseForMalformed() {
        assertFalse(jwtUtil.validateToken("nope"));
    }

    @Test
    void validateToken_falseForEmpty() {
        assertFalse(jwtUtil.validateToken(""));
    }

    @Test
    void validateToken_falseForWrongSignature() {
        String wrong = "differentsecretdifferentsecretdifferentsecret";
        String t = Jwts.builder()
                .setSubject("x@y.com")
                .setExpiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(wrong.getBytes()))
                .compact();
        assertFalse(jwtUtil.validateToken(t));
    }
}
