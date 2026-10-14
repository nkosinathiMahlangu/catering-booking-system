package codewithnathi.Catering.service;

import codewithnathi.Catering.config.JwtUtil;
import codewithnathi.Catering.dto.*;
import codewithnathi.Catering.entity.User;
import codewithnathi.Catering.repository.UserRepository;
import codewithnathi.Catering.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Orchestrates all authentication and identity flows for the catering system.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>User registration — delegates validation and persistence to {@link UserService}</li>
 *   <li>Login — authenticates credentials via Spring Security's {@link AuthenticationManager}
 *       and issues a JWT</li>
 *   <li>Forgot-password — triggers OTP generation and email dispatch</li>
 *   <li>OTP verification — checks the submitted code is still valid</li>
 *   <li>Password reset — applies a new password after successful OTP verification</li>
 * </ul>
 * </p>
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    /**
     * Registers a new user and returns an {@link AuthResponse} containing a JWT,
     * so the client is immediately signed in after successful registration.
     *
     * @param req registration payload (email, password, name, surname)
     * @return an {@link AuthResponse} with token, email, role, and name
     * @throws IllegalArgumentException if the email is taken or the password fails validation
     */
    public AuthResponse register(RegisterRequest req) {
        User user = userService.register(req);
        // Wrap the entity in UserPrincipal so JwtUtil can call getUsername() on it
        UserPrincipal principal = new UserPrincipal(user);
        String token = jwtUtil.generateToken(principal);
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .name(user.getName())
                .build();
    }

    /**
     * Authenticates an existing user and returns a fresh JWT.
     * <p>
     * Delegates credential verification to Spring Security's {@link AuthenticationManager},
     * which uses the DaoAuthenticationProvider (via SecurityConfig) under the hood.
     * If authentication fails, Spring throws {@link org.springframework.security.core.AuthenticationException}.
     * </p>
     *
     * @param req login payload (email and plain-text password)
     * @return an {@link AuthResponse} with a signed JWT and user details
     */
    public AuthResponse login(LoginRequest req) {
        // Throws BadCredentialsException / DisabledException if authentication fails
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );

        // Authentication passed — load the principal and unwrap to entity for the response
        UserPrincipal principal = (UserPrincipal) userService.loadUserByUsername(req.getEmail());
        User user = principal.getUser();
        String token = jwtUtil.generateToken(principal);
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .role(user.getRole().name())
                .name(user.getName())
                .build();
    }

    /**
     * Initiates a password reset by generating a 2-minute OTP and emailing it to the user.
     *
     * @param req contains the email address of the account to reset
     * @throws UsernameNotFoundException if no account exists for that email
     */
    public void forgotPassword(ForgotPasswordRequest req) {
        userService.initiatePasswordReset(req.getEmail());
    }

    /**
     * Completes a password reset after the user has verified their OTP.
     *
     * @param req contains email, the OTP code, and the desired new password
     * @throws IllegalArgumentException  if the OTP is invalid, expired, or the new password fails validation
     * @throws UsernameNotFoundException if the account does not exist
     */
    public void resetPassword(ResetPasswordRequest req) {
        userService.resetPassword(req);
    }

    /**
     * Verifies that the submitted OTP is valid for the given email address.
     * Used as a standalone step so the UI can gate the new-password form
     * behind a confirmed OTP.
     *
     * @param req contains the email and the OTP code to verify
     * @throws UsernameNotFoundException if no account exists for that email
     * @throws IllegalArgumentException  if the OTP has expired or does not match
     */
    public void verifyOtp(OtpRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + req.getEmail()));

        // Reject if OTP was never set or its 2-minute window has passed
        if (user.getOtpExpiry() == null || LocalDateTime.now().isAfter(user.getOtpExpiry())) {
            throw new IllegalArgumentException("OTP has expired. Please request a new one.");
        }

        if (!req.getOtpCode().equals(user.getResetOtpCode())) {
            throw new IllegalArgumentException("Invalid OTP code.");
        }
    }
}
