package com.yuksel.investmenttracker.controller;

import com.yuksel.investmenttracker.domain.enums.OAuthProvider;
import com.yuksel.investmenttracker.dto.request.ForgotPasswordRequest;
import com.yuksel.investmenttracker.dto.request.LoginRequest;
import com.yuksel.investmenttracker.dto.request.OAuthLoginRequest;
import com.yuksel.investmenttracker.dto.request.ResetPasswordRequest;
import com.yuksel.investmenttracker.dto.request.SignUpRequest;
import com.yuksel.investmenttracker.dto.response.AuthResponse;
import com.yuksel.investmenttracker.dto.response.UserResponse;
import com.yuksel.investmenttracker.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    @Operation(summary = "Register a new user")
    public ResponseEntity<AuthResponse> signUp(@Valid @RequestBody SignUpRequest signUpRequest) {
        AuthResponse authResponse = authService.signUp(signUpRequest);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse authResponse = authService.login(loginRequest);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/oauth/google")
    @Operation(summary = "Authenticate with Google")
    public ResponseEntity<AuthResponse> googleAuth(@Valid @RequestBody OAuthLoginRequest oauthRequest) {
        AuthResponse authResponse = authService.oauthLogin(oauthRequest, OAuthProvider.GOOGLE);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/oauth/apple")
    @Operation(summary = "Authenticate with Apple")
    public ResponseEntity<AuthResponse> appleAuth(@Valid @RequestBody OAuthLoginRequest oauthRequest) {
        AuthResponse authResponse = authService.oauthLogin(oauthRequest, OAuthProvider.APPLE);
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Send password reset email")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.sendPasswordResetEmail(request.getEmail());
        return ResponseEntity.ok("Password reset email sent. Check your email for instructions.");
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with token")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body("Passwords do not match");
        }
        
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok("Password reset successfully. Please login with your new password.");
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<UserResponse> getCurrentUser() {
        UserResponse userResponse = authService.getCurrentUser();
        return ResponseEntity.ok(userResponse);
    }
}