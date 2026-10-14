package codewithnathi.Catering.controller;

import codewithnathi.Catering.dto.PaymentRequest;
import codewithnathi.Catering.dto.PaymentResponse;
import codewithnathi.Catering.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for payment operations.
 * Both endpoints require authentication.
 */
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Processes a deposit or full payment for a booking.
     *
     * @param req            payment request (bookingId + payFull flag)
     * @param authentication Spring Security context — name is the user's email
     * @return 200 OK with the updated payment response
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(
            @Valid @RequestBody PaymentRequest req,
            Authentication authentication) {
        return ResponseEntity.ok(paymentService.processPayment(req, authentication.getName()));
    }

    /**
     * Returns the payment record for a specific booking.
     *
     * @param bookingId      the booking whose payment to retrieve
     * @param authentication Spring Security context
     * @return 200 OK with the payment response
     */
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentResponse> getPaymentForBooking(
            @PathVariable Long bookingId,
            Authentication authentication) {
        return ResponseEntity.ok(paymentService.getPaymentForBooking(bookingId, authentication.getName()));
    }
}
