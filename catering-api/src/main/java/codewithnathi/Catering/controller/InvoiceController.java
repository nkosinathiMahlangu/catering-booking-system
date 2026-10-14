package codewithnathi.Catering.controller;

import codewithnathi.Catering.entity.Invoice;
import codewithnathi.Catering.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for invoice retrieval.
 * Both endpoints require authentication (enforced by the catch-all
 * {@code anyRequest().authenticated()} rule in SecurityConfig).
 */
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    /**
     * Returns all invoices for a given booking.
     *
     * @param bookingId the booking whose invoices to retrieve
     * @return 200 OK with the list of invoices
     */
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<Invoice>> getInvoicesForBooking(@PathVariable Long bookingId) {
        return ResponseEntity.ok(invoiceService.getInvoicesForBooking(bookingId));
    }

    /**
     * Returns a single invoice by id.
     *
     * @param invoiceId the invoice id
     * @return 200 OK with the invoice
     */
    @GetMapping("/{invoiceId}")
    public ResponseEntity<Invoice> getInvoice(@PathVariable Long invoiceId) {
        return ResponseEntity.ok(invoiceService.getInvoice(invoiceId));
    }
}
