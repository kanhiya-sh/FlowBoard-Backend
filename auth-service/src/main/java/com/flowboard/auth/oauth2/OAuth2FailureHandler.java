package com.flowboard.auth.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.oauth2.failure-redirect-url}")
    private String failureRedirectUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.error("OAuth2 authentication failed: {}", exception.getMessage());
        String errorMsg = URLEncoder.encode(
            exception.getMessage() != null ? exception.getMessage() : "Authentication failed",
            StandardCharsets.UTF_8
        );
        // Redirect to /login?oauthError=... — matches LoginComponent's ngOnInit check
        getRedirectStrategy().sendRedirect(request, response,
            failureRedirectUrl + "?oauthError=" + errorMsg);
    }
}
