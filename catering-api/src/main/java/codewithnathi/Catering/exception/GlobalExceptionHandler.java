package codewithnathi.Catering.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Centralised exception handler for all REST controllers.
 * <p>
 * Converts exceptions into consistent JSON error responses so the client
 * always receives {@code {"error": "...", "message": "..."}} on failure.
 * </p>
 * <p>
 * Note: {@link BookingNotAllowedException} and {@link CancellationNotAllowedException}
 * are registered here as empty shells. Their full throw sites are implemented in FEAT-003.
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles business-rule violations raised by service methods
     * (e.g. duplicate email, invalid OTP, password failing validation).
     *
     * @return 400 Bad Request with the exception's message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handles lookups for entities that do not exist (e.g. booking or package not found).
     *
     * @return 404 Not Found
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleEntityNotFound(EntityNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handles Spring Security's UsernameNotFoundException which is thrown when a user
     * account cannot be found during authentication flows such as OTP verification or
     * password reset (verify-otp, reset-password endpoints).
     *
     * @return 404 Not Found
     */
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUsernameNotFound(UsernameNotFoundException ex) {
        return buildError(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handles attempts to create a booking that violates business rules
     * (too close to event, date fully booked). Implemented in FEAT-003.
     *
     * @return 409 Conflict
     */
    @ExceptionHandler(BookingNotAllowedException.class)
    public ResponseEntity<Map<String, String>> handleBookingNotAllowed(BookingNotAllowedException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles cancellation attempts that fall within the non-refundable window
     * (≤ 4 days before the event). Implemented in FEAT-003.
     * Note: a ≤ 3-day cancellation is still processed but the deposit is forfeited —
     * only truly blocked cancellations (≤ 4 days with no payment) throw this exception.
     *
     * @return 409 Conflict
     */
    @ExceptionHandler(CancellationNotAllowedException.class)
    public ResponseEntity<Map<String, String>> handleCancellationNotAllowed(CancellationNotAllowedException ex) {
        return buildError(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * Handles {@code @Valid} validation failures on request bodies.
     * Returns a map of field names to their individual error messages so the
     * client can highlight specific form fields.
     *
     * @return 400 Bad Request with a {@code fieldErrors} map
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        Map<String, Object> body = new HashMap<>();
        body.put("error", "Validation Failed");
        body.put("fieldErrors", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Handles Spring Security access denials when a user attempts an action
     * they are not authorised for (e.g. a USER hitting an ADMIN endpoint).
     *
     * @return 403 Forbidden
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return buildError(HttpStatus.FORBIDDEN, "Access denied: " + ex.getMessage());
    }

    /**
     * Catch-all for any unhandled exception. Hides internal details from the client
     * to avoid leaking stack traces or implementation details.
     *
     * @return 500 Internal Server Error with a generic message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }

    /**
     * Builds a standard two-field error body {@code {"error": statusPhrase, "message": message}}.
     *
     * @param status  the HTTP status to return
     * @param message the human-readable error description
     * @return a {@link ResponseEntity} wrapping the error map
     */
    private ResponseEntity<Map<String, String>> buildError(HttpStatus status, String message) {
        Map<String, String> body = new HashMap<>();
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
