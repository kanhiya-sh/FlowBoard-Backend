package com.flowboard.auth;

import com.flowboard.auth.dto.*;
import com.flowboard.auth.entity.User;
import com.flowboard.auth.repository.UserRepository;
import com.flowboard.auth.service.impl.AuthServiceImpl;
import com.flowboard.auth.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @InjectMocks private AuthServiceImpl authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1L).email("test@example.com").username("testuser")
                .passwordHash("encoded_pass").role("MEMBER").isActive(true).build();
    }

    @Test
    void register_success() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("new@example.com"); req.setUsername("newuser");
        req.setPassword("pass123"); req.setFullName("New User");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        String result = authService.register(req);
        assertNotNull(result);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_throwsWhenEmailExists() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("test@example.com"); req.setUsername("u"); req.setPassword("p");

        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
    }

    @Test
    void register_throwsWhenUsernameExists() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("new@example.com"); req.setUsername("testuser"); req.setPassword("p");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("testuser")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
    }

    @Test
    void login_success() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com"); req.setPassword("plainPass");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("plainPass", "encoded_pass")).thenReturn(true);
        when(jwtUtil.generateToken("test@example.com")).thenReturn("jwt_token");

        LoginResponse resp = authService.login(req);
        assertNotNull(resp);
        assertEquals("jwt_token", resp.getToken());
        assertEquals(1L, resp.getUserId());
    }

    @Test
    void login_wrongPassword_throws() {
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com"); req.setPassword("wrong");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrong", "encoded_pass")).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> authService.login(req));
    }

    @Test
    void login_inactiveUser_throws() {
        testUser.setActive(false);
        LoginRequest req = new LoginRequest();
        req.setEmail("test@example.com"); req.setPassword("pass");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        assertThrows(IllegalStateException.class, () -> authService.login(req));
    }

    @Test
    void getUserById_returnsDTO() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        UserResponseDTO dto = authService.getUserById(1L);
        assertNotNull(dto);
        assertEquals("test@example.com", dto.getEmail());
        assertEquals("testuser", dto.getUsername());
    }

    @Test
    void getUserById_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(RuntimeException.class, () -> authService.getUserById(99L));
    }

    @Test
    void deactivateAccount_setsInactive() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);
        authService.deactivateAccount(1L);
        assertFalse(testUser.isActive());
        verify(userRepository).save(testUser);
    }

    @Test
    void changePassword_success() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("old"); req.setNewPassword("newPass");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("old", "encoded_pass")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("new_hashed");
        when(userRepository.save(any())).thenReturn(testUser);

        authService.changePassword(1L, req);
        verify(userRepository).save(any());
    }

    // ─── login: user not found ────────────────────────────────────────────────

    @Test
    void login_userNotFound_throws() {
        LoginRequest req = new LoginRequest();
        req.setEmail("nope@e.com"); req.setPassword("x");
        when(userRepository.findByEmail("nope@e.com")).thenReturn(Optional.empty());

        assertThrows(com.flowboard.auth.exception.ResourceNotFoundException.class,
                () -> authService.login(req));
    }

    // ─── getUserByEmail / getUserByUsername / searchUsers ─────────────────────

    @Test
    void getUserByEmail_returnsDTO() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        UserResponseDTO dto = authService.getUserByEmail("test@example.com");
        assertEquals("testuser", dto.getUsername());
    }

    @Test
    void getUserByEmail_notFound_throws() {
        when(userRepository.findByEmail("x@y.com")).thenReturn(Optional.empty());
        assertThrows(com.flowboard.auth.exception.ResourceNotFoundException.class,
                () -> authService.getUserByEmail("x@y.com"));
    }

    @Test
    void getUserByUsername_returnsDTO() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        UserResponseDTO dto = authService.getUserByUsername("testuser");
        assertEquals(1L, dto.getUserId());
    }

    @Test
    void getUserByUsername_notFound_throws() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        assertThrows(com.flowboard.auth.exception.ResourceNotFoundException.class,
                () -> authService.getUserByUsername("ghost"));
    }

    @Test
    void searchUsers_returnsList() {
        when(userRepository.searchByFullName("test")).thenReturn(java.util.List.of(testUser));
        var result = authService.searchUsers("test");
        assertEquals(1, result.size());
    }

    // ─── updateProfile ────────────────────────────────────────────────────────

    @Test
    void updateProfile_success_changesAllFields() {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("New Name"); req.setUsername("newname"); req.setAvatarUrl("/a.png");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("newname")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponseDTO result = authService.updateProfile(1L, req);
        assertEquals("newname", result.getUsername());
    }

    @Test
    void updateProfile_blankFields_keepsOld() {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName(" "); req.setUsername(" "); // both blank → no-op
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponseDTO result = authService.updateProfile(1L, req);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void updateProfile_sameUsername_skipsUniqueCheck() {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setUsername("testuser"); // same as current
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponseDTO result = authService.updateProfile(1L, req);
        assertEquals("testuser", result.getUsername());
        verify(userRepository, never()).existsByUsername(anyString());
    }

    @Test
    void updateProfile_usernameTaken_throws() {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setUsername("taken");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("taken")).thenReturn(true);

        assertThrows(IllegalArgumentException.class,
                () -> authService.updateProfile(1L, req));
    }

    @Test
    void updateProfile_userNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(com.flowboard.auth.exception.ResourceNotFoundException.class,
                () -> authService.updateProfile(99L, new UpdateProfileRequest()));
    }

    // ─── changePassword ───────────────────────────────────────────────────────

    @Test
    void changePassword_wrongCurrent_throws() {
        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("bad"); req.setNewPassword("x");
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("bad", "encoded_pass")).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
                () -> authService.changePassword(1L, req));
    }

    @Test
    void changePassword_userNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(com.flowboard.auth.exception.ResourceNotFoundException.class,
                () -> authService.changePassword(99L, new ChangePasswordRequest()));
    }

    @Test
    void deactivateAccount_userNotFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(com.flowboard.auth.exception.ResourceNotFoundException.class,
                () -> authService.deactivateAccount(99L));
    }
}
