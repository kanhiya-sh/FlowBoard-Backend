package com.flowboard.auth;

import com.flowboard.auth.entity.User;
import com.flowboard.auth.security.CustomUserDetails;
import com.flowboard.auth.security.CustomUserDetailsService;
import com.flowboard.auth.security.JwtFilter;
import com.flowboard.auth.util.JwtUtil;
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

class JwtFilterTest {

    private JwtUtil jwtUtil;
    private CustomUserDetailsService uds;
    private JwtFilter filter;
    private FilterChain chain;

    @BeforeEach
    void setUp() {
        jwtUtil = mock(JwtUtil.class);
        uds = mock(CustomUserDetailsService.class);
        filter = new JwtFilter(uds, jwtUtil);
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
    void skippedPaths_passWithoutTokenCheck() throws Exception {
        // For paths that shouldNotFilter returns true, doFilter just passes through
        // without needing an Authorization header or JWT validation.
        for (String path : new String[]{"/auth/login", "/auth/register", "/oauth2/authorize", "/login/oauth2/code/google"}) {
            MockHttpServletRequest r = req(path);
            MockHttpServletResponse res = new MockHttpServletResponse();
            FilterChain c = mock(FilterChain.class);
            filter.doFilter(r, res, c);
            verify(c).doFilter(r, res);
            assertEquals(200, res.getStatus());
        }
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void noAuthHeader_passesThrough() throws Exception {
        MockHttpServletRequest r = req("/auth/profile/1");
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(r, res, chain);
        verify(chain).doFilter(r, res);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void invalidToken_returns401() throws Exception {
        MockHttpServletRequest r = req("/auth/profile/1");
        r.addHeader("Authorization", "Bearer bad");
        MockHttpServletResponse res = new MockHttpServletResponse();
        when(jwtUtil.extractEmail("bad")).thenReturn("a@b.com");
        when(jwtUtil.validateToken("bad")).thenReturn(false);

        filter.doFilter(r, res, chain);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, res.getStatus());
    }

    @Test
    void validToken_setsAuthentication() throws Exception {
        MockHttpServletRequest r = req("/auth/profile/1");
        r.addHeader("Authorization", "Bearer good");
        MockHttpServletResponse res = new MockHttpServletResponse();

        when(jwtUtil.extractEmail("good")).thenReturn("a@b.com");
        when(jwtUtil.validateToken("good")).thenReturn(true);
        User u = User.builder().email("a@b.com").passwordHash("h").role("MEMBER").isActive(true).build();
        when(uds.loadUserByUsername("a@b.com")).thenReturn(new CustomUserDetails(u));

        filter.doFilter(r, res, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain).doFilter(r, res);
    }

    @Test
    void parsingException_returns401() throws Exception {
        MockHttpServletRequest r = req("/auth/profile/1");
        r.addHeader("Authorization", "Bearer broken");
        MockHttpServletResponse res = new MockHttpServletResponse();
        when(jwtUtil.extractEmail("broken")).thenThrow(new RuntimeException("bad"));

        filter.doFilter(r, res, chain);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, res.getStatus());
    }
}
