package codewithnathi.Catering.controller;

import codewithnathi.Catering.dto.BookingRequest;
import codewithnathi.Catering.dto.BookingResponse;
import codewithnathi.Catering.dto.CancellationResponse;
import codewithnathi.Catering.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for booking operations.
 * All endpoints require authentication; the admin-only endpoint is further
 * guarded by {@code @PreAuthorize("hasRole('ADMIN')")} (enabled via
 * {@code @EnableMethodSecurity} on {@link codewithnathi.Catering.config.SecurityConfig}).
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    /**
     * Creates a new booking for the authenticated user.
     *
     * @param req            validated booking payload
     * @param authentication Spring Security context — name is the user's email
     * @return 201 Created with the booking response
     */
    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingRequest req,
            Authentication authentication) {
        BookingResponse response = bookingService.createBooking(req, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Returns all bookings belonging to the authenticated user.
     *
     * @param authentication Spring Security context
     * @return 200 OK with the list of bookings
     */
    @GetMapping
    public ResponseEntity<List<BookingResponse>> getUserBookings(Authentication authentication) {
        return ResponseEntity.ok(bookingService.getUserBookings(authentication.getName()));
    }

    /**
     * Returns a single booking by id. Regular users may only fetch their own bookings.
     *
     * @param id             booking id
     * @param authentication Spring Security context
     * @return 200 OK with the booking response
     */
    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getBooking(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(bookingService.getBooking(id, authentication.getName()));
    }

    /**
     * Cancels a booking and applies the refund matrix.
     *
     * @param id             booking id to cancel
     * @param authentication Spring Security context
     * @return 200 OK with the cancellation response including refund details
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<CancellationResponse> cancelBooking(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(bookingService.cancelBooking(id, authentication.getName()));
    }

    /**
     * Admin-only: returns all bookings in the system.
     *
     * @return 200 OK with the full list of bookings
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BookingResponse>> getAllBookings() {
        return ResponseEntity.ok(bookingService.getAllBookings());
    }
}
