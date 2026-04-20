package com.flowboard.auth.service;

import com.flowboard.auth.dto.LoginRequest;
import com.flowboard.auth.dto.LoginResponse;
import com.flowboard.auth.dto.RegisterRequest;

public interface AuthService {

    String register(RegisterRequest request);

    LoginResponse login(LoginRequest request);
}