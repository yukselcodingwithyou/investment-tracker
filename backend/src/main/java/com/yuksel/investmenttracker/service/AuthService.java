package com.yuksel.investmenttracker.service;

import com.yuksel.investmenttracker.domain.entity.AuthToken;
import com.yuksel.investmenttracker.domain.entity.User;
import com.yuksel.investmenttracker.domain.enums.OAuthProvider;
import com.yuksel.investmenttracker.dto.request.LoginRequest;
import com.yuksel.investmenttracker.dto.request.OAuthLoginRequest;
import com.yuksel.investmenttracker.dto.request.SignUpRequest;
import com.yuksel.investmenttracker.dto.response.AuthResponse;
import com.yuksel.investmenttracker.dto.response.UserResponse;
import com.yuksel.investmenttracker.repository.AuthTokenRepository;
import com.yuksel.investmenttracker.repository.UserRepository;
import com.yuksel.investmenttracker.security.JwtTokenProvider;
import com.yuksel.investmenttracker.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public AuthResponse signUp(SignUpRequest signUpRequest) {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new RuntimeException("Email address already in use.");
        }

        if (!signUpRequest.getPassword().equals(signUpRequest.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match.");
        }

        // Create new user
        User user = new User();
        user.setName(signUpRequest.getName());
        user.setEmail(signUpRequest.getEmail());
        user.setPasswordHash(passwordEncoder.encode(signUpRequest.getPassword()));
        user.setEmailVerified(false);
        user.setProviders(new ArrayList<>());
        user.setBaseCurrency("TRY");
        user.setTimezone("Europe/Istanbul");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        // Create authentication
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        signUpRequest.getEmail(),
                        signUpRequest.getPassword()
                )
        );

        return createAuthResponse(authentication);
    }

    public AuthResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmailOrUsername(),
                        loginRequest.getPassword()
                )
        );

        return createAuthResponse(authentication);
    }

    @Transactional
    public AuthResponse oauthLogin(OAuthLoginRequest oauthRequest, OAuthProvider provider) {
        // TODO: Implement OAuth token validation for Google and Apple
        // For now, this is a placeholder
        throw new RuntimeException("OAuth login not yet implemented");
    }

    public void logout(String userId) {
        authTokenRepository.deleteByUserId(userId);
    }

    private AuthResponse createAuthResponse(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String jwt = tokenProvider.generateToken(authentication);
        String refreshToken = tokenProvider.generateRefreshToken(userPrincipal.getId());

        // Save refresh token
        AuthToken authToken = new AuthToken();
        authToken.setUserId(userPrincipal.getId());
        authToken.setRefreshTokenHash(passwordEncoder.encode(refreshToken));
        authToken.setExpiresAt(LocalDateTime.now().plusDays(7));
        authToken.setCreatedAt(LocalDateTime.now());
        authTokenRepository.save(authToken);

        // Create user response
        UserResponse userResponse = new UserResponse();
        userResponse.setId(userPrincipal.getId());
        userResponse.setName(userPrincipal.getName());
        userResponse.setEmail(userPrincipal.getEmail());
        userResponse.setProviders(userPrincipal.getProviders());

        return new AuthResponse(jwt, refreshToken, userResponse);
    }

    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserResponse userResponse = new UserResponse();
        userResponse.setId(user.getId());
        userResponse.setName(user.getName());
        userResponse.setEmail(user.getEmail());
        userResponse.setEmailVerified(user.isEmailVerified());
        userResponse.setProviders(user.getProviders());
        userResponse.setBaseCurrency(user.getBaseCurrency());
        userResponse.setTimezone(user.getTimezone());
        userResponse.setCreatedAt(user.getCreatedAt());

        return userResponse;
    }
}