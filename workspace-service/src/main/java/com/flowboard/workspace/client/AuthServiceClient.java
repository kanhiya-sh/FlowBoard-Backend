package com.flowboard.workspace.client;

import com.flowboard.workspace.dto.UserResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @GetMapping("/auth/internal/users/email/{email}")
    UserResponseDTO getUserByEmail(@PathVariable("email") String email);

    @GetMapping("/auth/internal/users/{userId}")
    UserResponseDTO getUserById(@PathVariable("userId") Long userId);
}
