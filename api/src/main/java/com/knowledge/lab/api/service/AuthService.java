package com.knowledge.lab.api.service;

import com.knowledge.lab.api.config.JWTConfig;
import com.knowledge.lab.api.dto.request.LoginRequest;
import com.knowledge.lab.api.dto.request.RegisterRequest;
import com.knowledge.lab.api.dto.response.AuthResponse;
import com.knowledge.lab.api.model.User;
import com.knowledge.lab.api.repository.UserRepository;
import com.knowledge.lab.api.utility.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository        userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JWTUtil               jwtUtil;
    private final JWTConfig             jwtProperties;
    private final RefreshTokenService   refreshTokenService;
    private final AuthenticationManager authenticationManager;

    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        var user = User.builder()
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .firstName(req.firstName())
                .lastName(req.lastName())
                .build();

        user = userRepository.save(user);
        log.info("Registered new user: {}", user.getEmail());

        return issueTokens(user);
    }

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );

        var user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new IllegalStateException("User not found after authentication"));

        log.info("User logged in: {}", user.getEmail());
        return issueTokens(user);
    }

    /** Rotate refresh token and issue a new access token. */
    public AuthResponse refresh(String refreshTokenId) {
        var newRefreshToken = refreshTokenService.rotate(refreshTokenId);

        var user = userRepository.findByEmail(newRefreshToken.getEmail())
                .orElseThrow(() -> new IllegalStateException("User not found for refresh token"));

        var accessToken = jwtUtil.generateAccessToken(
                Map.of("roles", user.getRoles()),
                buildUserDetails(user)
        );

        return AuthResponse.of(accessToken, newRefreshToken.getId(), jwtProperties.getAccessTokenExpiryMs());
    }

    public void logout(String refreshTokenId) {
        refreshTokenService.revokeToken(refreshTokenId);
    }

    public void logoutAll(String email) {
        userRepository.findByEmail(email).ifPresent(user ->
                refreshTokenService.revokeAllForUser(user.getId())
        );
    }

    private AuthResponse issueTokens(User user) {
        var userDetails  = buildUserDetails(user);
        var accessToken  = jwtUtil.generateAccessToken(Map.of("roles", user.getRoles()), userDetails);
        var refreshToken = refreshTokenService.create(user.getId(), user.getEmail());
        return AuthResponse.of(accessToken, refreshToken.getId(), jwtProperties.getAccessTokenExpiryMs());
    }

    private org.springframework.security.core.userdetails.User buildUserDetails(User user) {
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.getRoles().stream()
                        .map(r -> new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + r.name()))
                        .toList()
        );
    }
}
