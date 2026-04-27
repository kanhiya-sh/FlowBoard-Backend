package com.flowboard.comment.config;

import com.flowboard.comment.dto.UserResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service", url = "${auth.service.url}")
public interface AuthServiceClient {
    @GetMapping("/auth/internal/users/email/{email}")
    UserResponseDTO getUserByEmail(@PathVariable("email") String email);

    @GetMapping("/auth/internal/users/{userId}")
    UserResponseDTO getUserById(@PathVariable("userId") Long userId);

    @GetMapping("/auth/internal/users/username/{username}")
    UserResponseDTO getUserByUsername(@PathVariable("username") String username);
}
