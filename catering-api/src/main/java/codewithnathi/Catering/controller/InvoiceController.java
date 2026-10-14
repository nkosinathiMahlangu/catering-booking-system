package codewithnathi.Catering.controller;

import codewithnathi.Catering.dto.InvoiceResponse;
import codewithnathi.Catering.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for invoice retrieval.
 * Both endpoints require authentication (enforced by the catch-all
 * {@code anyRequest().authenticated()} rule in SecurityConfig).
 * Ownership is enforced in {@link InvoiceService}: regular users can only
 * retrieve invoices for their own bookings; admins can access all.
 */
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    /**
     * Returns all invoices for a given booking.
     *
     * @param bookingId      the booking whose invoices to retrieve
     * @param authentication Spring Security context — name is the user's email
     * @return 200 OK with the list of invoice responses
     */
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesForBooking(
            @PathVariable Long bookingId,
            Authentication authentication) {
        return ResponseEntity.ok(invoiceService.getInvoicesForBooking(bookingId, authentication.getName()));
    }

    /**
     * Returns a single invoice by id.
     *
     * @param invoiceId      the invoice id
     * @param authentication Spring Security context — name is the user's email
     * @return 200 OK with the invoice response
     */
    @GetMapping("/{invoiceId}")
    public ResponseEntity<InvoiceResponse> getInvoice(
            @PathVariable Long invoiceId,
            Authentication authentication) {
        return ResponseEntity.ok(invoiceService.getInvoice(invoiceId, authentication.getName()));
    }
}
