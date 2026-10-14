package codewithnathi.Catering.controller;

import codewithnathi.Catering.dto.*;
import codewithnathi.Catering.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for all authentication and identity endpoints.
 * All routes under {@code /api/auth} are publicly accessible (no JWT required),
 * as configured in {@link codewithnathi.Catering.config.SecurityConfig}.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Registers a new user account and returns a JWT so the client is immediately
     * signed in after registration.
     *
     * @param request validated registration payload
     * @return 201 Created with an {@link AuthResponse} containing the token and user info
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * Authenticates an existing user and returns a fresh JWT.
     *
     * @param request login credentials (email and password)
     * @return 200 OK with an {@link AuthResponse} containing the token and user info
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Triggers the forgot-password flow: generates an OTP and emails it to the user.
     *
     * @param request the email address to send the OTP to
     * @return 200 OK (no body — avoids leaking whether the email exists)
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Verifies the OTP submitted by the user during the password-reset flow.
     * The UI should call this before presenting the new-password form.
     *
     * @param request email and OTP code
     * @return 200 OK if the OTP is valid and not expired
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<Void> verifyOtp(@RequestBody OtpRequest request) {
        authService.verifyOtp(request);
        return ResponseEntity.ok().build();
    }

    /**
     * Resets the user's password after a valid OTP has been verified.
     *
     * @param request email, OTP code, and the desired new password
     * @return 200 OK on success
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }
}
