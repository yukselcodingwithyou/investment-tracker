package com.yuksel.investmenttracker.service;

import com.yuksel.investmenttracker.domain.entity.AuthToken;
import com.yuksel.investmenttracker.domain.entity.PasswordResetToken;
import com.yuksel.investmenttracker.domain.entity.User;
import com.yuksel.investmenttracker.domain.enums.OAuthProvider;
import com.yuksel.investmenttracker.dto.OAuthUserInfo;
import com.yuksel.investmenttracker.dto.request.LoginRequest;
import com.yuksel.investmenttracker.dto.request.OAuthLoginRequest;
import com.yuksel.investmenttracker.dto.request.SignUpRequest;
import com.yuksel.investmenttracker.dto.response.AuthResponse;
import com.yuksel.investmenttracker.dto.response.UserResponse;
import com.yuksel.investmenttracker.repository.AuthTokenRepository;
import com.yuksel.investmenttracker.repository.PasswordResetTokenRepository;
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

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final OAuthService oauthService;
    private final EmailService emailService;

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
        // Validate OAuth token with provider
        OAuthUserInfo userInfo = oauthService.validateOAuthToken(oauthRequest.getToken(), provider);
        
        // Find or create user
        User user = userRepository.findByEmail(userInfo.getEmail())
                .orElseGet(() -> createUserFromOAuth(userInfo, provider));
        
        // Update providers list if not already present
        if (!user.getProviders().contains(provider)) {
            user.getProviders().add(provider);
            userRepository.save(user);
        }
        
        // Generate tokens and return auth response
        return createAuthResponse(createAuthentication(user));
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
    
    @Transactional
    public void sendPasswordResetEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
        
        // Delete any existing password reset tokens for this user
        passwordResetTokenRepository.deleteByUserId(user.getId());
        
        // Generate secure reset token
        String resetToken = generateSecureToken();
        
        // Create password reset token entity
        PasswordResetToken tokenEntity = new PasswordResetToken();
        tokenEntity.setUserId(user.getId());
        tokenEntity.setToken(resetToken);
        tokenEntity.setExpiresAt(LocalDateTime.now().plusHours(1)); // 1 hour expiry
        tokenEntity.setUsed(false);
        tokenEntity.setCreatedAt(LocalDateTime.now());
        
        passwordResetTokenRepository.save(tokenEntity);
        
        // Send email
        emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
        
        log.info("Password reset email sent to user: {}", email);
    }
    
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenAndUsedFalseAndExpiresAtAfter(token, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));
        
        User user = userRepository.findById(resetToken.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        // Mark token as used
        resetToken.setUsed(true);
        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepository.save(resetToken);
        
        // Delete all auth tokens to force re-login
        authTokenRepository.deleteByUserId(user.getId());
        
        log.info("Password reset successfully for user: {}", user.getEmail());
    }
    
    private User createUserFromOAuth(OAuthUserInfo userInfo, OAuthProvider provider) {
        User user = new User();
        user.setName(userInfo.getName());
        user.setEmail(userInfo.getEmail());
        user.setEmailVerified(userInfo.isEmailVerified());
        user.setPasswordHash(null); // OAuth users don't have passwords
        user.setProviders(new ArrayList<>());
        user.getProviders().add(provider);
        user.setBaseCurrency("TRY");
        user.setTimezone("Europe/Istanbul");
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        
        return userRepository.save(user);
    }
    
    private Authentication createAuthentication(User user) {
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        return new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
    }
    
    private String generateSecureToken() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}