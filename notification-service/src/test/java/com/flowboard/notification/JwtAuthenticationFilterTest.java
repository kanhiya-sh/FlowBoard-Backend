package com.flowboard.notification;

import com.flowboard.notification.security.JwtAuthenticationFilter;
import com.flowboard.notification.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {

    private JwtUtil jwtUtil;
    private JwtAuthenticationFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        jwtUtil = mock(JwtUtil.class);
        filter = new JwtAuthenticationFilter(jwtUtil);
        chain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void internalPath_skipsAuthAndContinuesChain() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/notifications/internal/send");
        req.setServletPath("/notifications/internal/send");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        verify(chain).doFilter(req, res);
        verifyNoInteractions(jwtUtil);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void missingAuthHeader_returns401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/notifications");
        req.setServletPath("/notifications");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, res.getStatus());
        assertTrue(res.getContentAsString().contains("Missing or invalid Authorization header"));
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void wrongPrefix_returns401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/notifications");
        req.setServletPath("/notifications");
        req.addHeader("Authorization", "Token abc123");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilter(req, res, chain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, res.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void invalidToken_returns401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/notifications");
        req.setServletPath("/notifications");
        req.addHeader("Authorization", "Bearer expired.token.here");
        MockHttpServletResponse res = new MockHttpServletResponse();
        when(jwtUtil.validateToken("expired.token.here")).thenReturn(false);

        filter.doFilter(req, res, chain);

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, res.getStatus());
        assertTrue(res.getContentAsString().contains("Invalid or expired JWT token"));
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void validToken_setsAttributesAndContinuesChain() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/notifications");
        req.setServletPath("/notifications");
        req.addHeader("Authorization", "Bearer valid.token");
        MockHttpServletResponse res = new MockHttpServletResponse();
        when(jwtUtil.validateToken("valid.token")).thenReturn(true);
        when(jwtUtil.extractEmail("valid.token")).thenReturn("alice@test.com");

        filter.doFilter(req, res, chain);

        assertEquals(HttpServletResponse.SC_OK, res.getStatus());
        assertEquals("alice@test.com", req.getAttribute("userEmail"));
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("alice@test.com", SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(chain).doFilter(req, res);
    }
}
