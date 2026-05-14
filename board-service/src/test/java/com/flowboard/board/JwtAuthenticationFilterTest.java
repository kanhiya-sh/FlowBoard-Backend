package com.flowboard.board;

import com.flowboard.board.security.JwtAuthenticationFilter;
import com.flowboard.board.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    private MockHttpServletRequest req(String path) {
        MockHttpServletRequest r = new MockHttpServletRequest("GET", path);
        r.setServletPath(path);
        return r;
    }

    @Test
    void internalPath_skipsAuth() throws Exception {
        MockHttpServletRequest r = req("/boards/internal/1/members/2/check");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(r, res, chain);
        verify(chain).doFilter(r, res);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void missingAuthHeader_returns401() throws Exception {
        MockHttpServletRequest r = req("/boards");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(r, res, chain);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, res.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void wrongPrefix_returns401() throws Exception {
        MockHttpServletRequest r = req("/boards");
        r.addHeader("Authorization", "Token abc");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(r, res, chain);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, res.getStatus());
    }

    @Test
    void invalidToken_returns401() throws Exception {
        MockHttpServletRequest r = req("/boards");
        r.addHeader("Authorization", "Bearer bad");
        MockHttpServletResponse res = new MockHttpServletResponse();
        when(jwtUtil.validateToken("bad")).thenReturn(false);
        filter.doFilter(r, res, chain);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, res.getStatus());
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void validToken_setsAttributesAndContinues() throws Exception {
        MockHttpServletRequest r = req("/boards");
        r.addHeader("Authorization", "Bearer good");
        MockHttpServletResponse res = new MockHttpServletResponse();
        when(jwtUtil.validateToken("good")).thenReturn(true);
        when(jwtUtil.extractEmail("good")).thenReturn("u@e.com");
        filter.doFilter(r, res, chain);
        assertEquals("u@e.com", r.getAttribute("userEmail"));
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(r, res);
    }
}
