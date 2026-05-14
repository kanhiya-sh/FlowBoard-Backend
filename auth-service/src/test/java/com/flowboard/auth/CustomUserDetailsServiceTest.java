package com.flowboard.auth;

import com.flowboard.auth.entity.User;
import com.flowboard.auth.repository.UserRepository;
import com.flowboard.auth.security.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomUserDetailsServiceTest {

    @Test
    void loadUserByUsername_returnsDetails() {
        UserRepository repo = mock(UserRepository.class);
        CustomUserDetailsService svc = new CustomUserDetailsService();
        ReflectionTestUtils.setField(svc, "userRepository", repo);

        User u = User.builder().email("a@b.com").passwordHash("h").role("MEMBER").isActive(true).build();
        when(repo.findByEmail("a@b.com")).thenReturn(Optional.of(u));

        UserDetails details = svc.loadUserByUsername("a@b.com");
        assertEquals("a@b.com", details.getUsername());
        assertEquals("h", details.getPassword());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MEMBER")));
        assertTrue(details.isEnabled());
        assertTrue(details.isAccountNonExpired());
        assertTrue(details.isAccountNonLocked());
        assertTrue(details.isCredentialsNonExpired());
    }

    @Test
    void loadUserByUsername_notFound_throws() {
        UserRepository repo = mock(UserRepository.class);
        CustomUserDetailsService svc = new CustomUserDetailsService();
        ReflectionTestUtils.setField(svc, "userRepository", repo);

        when(repo.findByEmail("x@y.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> svc.loadUserByUsername("x@y.com"));
    }
}
