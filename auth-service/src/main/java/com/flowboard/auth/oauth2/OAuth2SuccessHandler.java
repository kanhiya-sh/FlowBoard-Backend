package com.flowboard.auth.oauth2;

import com.flowboard.auth.entity.User;
import com.flowboard.auth.repository.UserRepository;
import com.flowboard.auth.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Value("${app.oauth2.success-redirect-url}")
    private String successRedirectUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        String email   = oAuth2User.getAttribute("email");
        String name    = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        if (email == null || email.isBlank()) {
            log.error("OAuth2 login failed: Google did not return an email address");
            getRedirectStrategy().sendRedirect(request, response,
                    "http://localhost:4200/login?oauthError=no_email");
            return;
        }

        try {
            Optional<User> existing = userRepository.findByEmail(email);
            User user;

            if (existing.isPresent()) {
                user = existing.get();
                if (picture != null) user.setAvatarUrl(picture);
                userRepository.save(user);
                log.info("OAuth2 login: existing user {}", email);
            } else {
                String username = generateUniqueUsername(email);
                user = User.builder()
                        .email(email)
                        .fullName(name != null ? name : email.split("@")[0])
                        .username(username)
                        .passwordHash(null)
                        .role("MEMBER")
                        .avatarUrl(picture)
                        .provider("GOOGLE")
                        .isActive(true)
                        .build();
                userRepository.save(user);
                log.info("OAuth2 login: new user registered {}", email);
            }

            String token = jwtUtil.generateToken(user.getEmail());

            // Redirect to Angular callback — include token + userId + email
            // Angular will use token directly (no need to call backend again for user info)
            // This avoids the CORS issue where completeOAuthLogin() was calling
            // /auth/internal/users/email directly from auth-service origin (8081)
            String redirectUrl = successRedirectUrl
                    + "?token=" + token
                    + "&userId=" + user.getUserId()
                    + "&email=" + encode(user.getEmail())
                    + "&fullName=" + encode(user.getFullName() != null ? user.getFullName() : "")
                    + "&username=" + encode(user.getUsername())
                    + "&role=" + encode(user.getRole())
                    + "&avatarUrl=" + encode(user.getAvatarUrl() != null ? user.getAvatarUrl() : "");

            log.info("OAuth2 success, redirecting to Angular callback for user: {}", email);
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);

        } catch (Exception e) {
            log.error("OAuth2 success handler error", e);
            getRedirectStrategy().sendRedirect(request, response,
                    "http://localhost:4200/login?oauthError=server_error");
        }
    }

    private String encode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    private String generateUniqueUsername(String email) {
        String base = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "");
        if (base.isEmpty()) base = "user";
        String username = base;
        int i = 1;
        while (userRepository.findByUsername(username).isPresent()) {
            username = base + i++;
        }
        return username;
    }
}
