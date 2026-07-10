package com.hackathon.gares.controller;

import com.hackathon.gares.dto.AuthResponse;
import com.hackathon.gares.dto.ForgotPasswordRequest;
import com.hackathon.gares.dto.ForgotPasswordResponse;
import com.hackathon.gares.dto.LoginRequest;
import com.hackathon.gares.dto.RegisterRequest;
import com.hackathon.gares.dto.RegisterResponse;
import com.hackathon.gares.dto.ResendVerificationRequest;
import com.hackathon.gares.dto.ResetPasswordRequest;
import com.hackathon.gares.dto.ResetPasswordResponse;
import com.hackathon.gares.dto.VerifyEmailRequest;
import com.hackathon.gares.dto.VerifyEmailResponse;
import com.hackathon.gares.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<VerifyEmailResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(authService.verifyEmail(request.email(), request.code()));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<RegisterResponse> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        return ResponseEntity.ok(authService.resendVerification(request.email()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ResetPasswordResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }
}
