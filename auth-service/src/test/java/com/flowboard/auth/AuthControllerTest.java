package com.flowboard.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowboard.auth.controller.AuthController;
import com.flowboard.auth.dto.*;
import com.flowboard.auth.security.CustomUserDetailsService;
import com.flowboard.auth.service.AuthService;
import com.flowboard.auth.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private AuthService authService;
    @MockBean  private CustomUserDetailsService userDetailsService;
    @MockBean  private JwtUtil jwtUtil;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void register_returns200() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("user@test.com"); req.setUsername("user1");
        req.setPassword("pass123"); req.setFullName("Test User");

        when(authService.register(any())).thenReturn("User registered successfully");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"));
    }

    @Test
    void login_returns200WithToken() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("user@test.com"); req.setPassword("pass123");

        LoginResponse resp = new LoginResponse("mock_jwt", 1L, "user@test.com", "Test User", "user1", "MEMBER", null, true, "Login successful");
        when(authService.login(any())).thenReturn(resp);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock_jwt"))
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    @WithMockUser
    void searchUsers_returns200() throws Exception {
        UserResponseDTO dto = UserResponseDTO.builder()
                .userId(1L).email("test@test.com").username("testuser").build();
        when(authService.searchUsers("test")).thenReturn(List.of(dto));

        mockMvc.perform(get("/auth/search").param("q", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("test@test.com"));
    }

    @Test
    @WithMockUser
    void getUserById_returns200() throws Exception {
        UserResponseDTO dto = UserResponseDTO.builder()
                .userId(1L).email("test@test.com").username("testuser").build();
        when(authService.getUserById(1L)).thenReturn(dto);

        mockMvc.perform(get("/auth/internal/users/1"))
                .andExpect(status().isOk());
    }
}
