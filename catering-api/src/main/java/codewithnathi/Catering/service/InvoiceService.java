package codewithnathi.Catering.service;

import codewithnathi.Catering.dto.InvoiceResponse;
import codewithnathi.Catering.entity.Booking;
import codewithnathi.Catering.entity.Invoice;
import codewithnathi.Catering.entity.Payment;
import codewithnathi.Catering.enums.InvoiceType;
import codewithnathi.Catering.enums.Role;
import codewithnathi.Catering.exception.BookingNotAllowedException;
import codewithnathi.Catering.repository.InvoiceRepository;
import codewithnathi.Catering.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Generates and persists invoices at key points in the booking lifecycle.
 * <p>
 * Invoice numbering: saved first to obtain the auto-generated id, then the
 * invoiceNumber is computed as {@code INV-{year}-{05d id}} and saved again.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final EmailService emailService;
    private final UserRepository userRepository;

    /**
     * Creates a booking-confirmation invoice when the deposit is paid.
     * Sent to the user's registered email.
     * <p>
     * Both saves run within a single transaction so no concurrent reader
     * can observe an invoice with a null {@code invoiceNumber}.
     * </p>
     *
     * @param booking the booking being confirmed
     * @param payment the associated payment record
     * @return the saved {@link Invoice}
     */
    @Transactional
    public Invoice createBookingConfirmation(Booking booking, Payment payment) {
        Invoice invoice = Invoice.builder()
                .booking(booking)
                .invoiceType(InvoiceType.BOOKING_CONFIRMATION)
                .invoiceDate(LocalDate.now())
                .totalAmount(payment.getTotalAmount())
                .depositAmount(payment.getDepositAmount())
                .balanceDue(payment.getBalanceDue())
                .refundAmount(payment.getRefundAmount())
                .notes("Deposit paid. Balance due: R" + payment.getBalanceDue())
                .build();

        invoice = invoiceRepository.save(invoice);
        invoice.setInvoiceNumber(buildInvoiceNumber(invoice.getId()));
        invoice = invoiceRepository.save(invoice);

        sendInvoiceEmail(booking, invoice, "Booking Confirmation - " + invoice.getInvoiceNumber());
        return invoice;
    }

    /**
     * Creates a payment-receipt invoice when the full amount is paid.
     * Both saves run within a single transaction so no concurrent reader
     * can observe an invoice with a null {@code invoiceNumber}.
     *
     * @param booking the booking that has been fully paid
     * @param payment the associated payment record
     * @return the saved {@link Invoice}
     */
    @Transactional
    public Invoice createPaymentReceipt(Booking booking, Payment payment) {
        Invoice invoice = Invoice.builder()
                .booking(booking)
                .invoiceType(InvoiceType.PAYMENT_RECEIPT)
                .invoiceDate(LocalDate.now())
                .totalAmount(payment.getTotalAmount())
                .depositAmount(payment.getDepositAmount())
                .balanceDue(payment.getBalanceDue())
                .refundAmount(payment.getRefundAmount())
                .notes("Paid in full.")
                .build();

        invoice = invoiceRepository.save(invoice);
        invoice.setInvoiceNumber(buildInvoiceNumber(invoice.getId()));
        invoice = invoiceRepository.save(invoice);

        sendInvoiceEmail(booking, invoice, "Payment Receipt - " + invoice.getInvoiceNumber());
        return invoice;
    }

    /**
     * Creates a cancellation-statement invoice showing what was paid, what is
     * forfeited, and what is refunded.
     * Both saves run within a single transaction so no concurrent reader
     * can observe an invoice with a null {@code invoiceNumber}.
     *
     * @param booking the booking being cancelled
     * @param payment the associated payment record (must already have refundAmount set)
     * @return the saved {@link Invoice}
     */
    @Transactional
    public Invoice createCancellationStatement(Booking booking, Payment payment) {
        String notes = buildCancellationNotes(payment);

        Invoice invoice = Invoice.builder()
                .booking(booking)
                .invoiceType(InvoiceType.CANCELLATION_STATEMENT)
                .invoiceDate(LocalDate.now())
                .totalAmount(payment.getTotalAmount())
                .depositAmount(payment.getDepositAmount())
                .balanceDue(payment.getBalanceDue())
                .refundAmount(payment.getRefundAmount())
                .notes(notes)
                .build();

        invoice = invoiceRepository.save(invoice);
        invoice.setInvoiceNumber(buildInvoiceNumber(invoice.getId()));
        invoice = invoiceRepository.save(invoice);

        sendInvoiceEmail(booking, invoice, "Cancellation Statement - " + invoice.getInvoiceNumber());
        return invoice;
    }

    /**
     * Returns all invoices associated with a booking.
     * Enforces ownership: regular users may only retrieve invoices for their own bookings.
     * Admins may access any booking's invoices.
     *
     * @param bookingId the booking id
     * @param userEmail email of the authenticated caller
     * @return list of invoice responses (may be empty)
     */
    public List<InvoiceResponse> getInvoicesForBooking(Long bookingId, String userEmail) {
        List<Invoice> invoices = invoiceRepository.findByBooking_Id(bookingId);
        // Check ownership on the first invoice's booking (or fall back to an empty-list check)
        if (!invoices.isEmpty()) {
            enforceOwnership(invoices.get(0).getBooking(), userEmail);
        }
        return invoices.stream().map(this::toResponse).toList();
    }

    /**
     * Returns a single invoice by id.
     * Enforces ownership: regular users may only retrieve their own invoices.
     * Admins may access any invoice.
     *
     * @param invoiceId the invoice id
     * @param userEmail email of the authenticated caller
     * @return the matching {@link InvoiceResponse}
     * @throws EntityNotFoundException if no invoice with that id exists
     */
    public InvoiceResponse getInvoice(Long invoiceId, String userEmail) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found with id: " + invoiceId));
        enforceOwnership(invoice.getBooking(), userEmail);
        return toResponse(invoice);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Maps an {@link Invoice} entity to a safe {@link InvoiceResponse} DTO.
     * Extracts only scalar fields from the associated {@link Booking} so that
     * no entity graph (including the User password hash) is ever exposed.
     */
    private InvoiceResponse toResponse(Invoice invoice) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .invoiceType(invoice.getInvoiceType())
                .invoiceDate(invoice.getInvoiceDate())
                .bookingId(invoice.getBooking() != null ? invoice.getBooking().getId() : null)
                .eventDate(invoice.getBooking() != null ? invoice.getBooking().getEventDate() : null)
                .totalAmount(invoice.getTotalAmount())
                .depositAmount(invoice.getDepositAmount())
                .balanceDue(invoice.getBalanceDue())
                .refundAmount(invoice.getRefundAmount())
                .notes(invoice.getNotes())
                .createdAt(invoice.getCreatedAt())
                .build();
    }

    private String buildInvoiceNumber(Long id) {
        int year = LocalDate.now().getYear();
        return "INV-" + year + "-" + String.format("%05d", id);
    }

    private String buildCancellationNotes(Payment payment) {
        if (payment.getRefundAmount() == null ||
                payment.getRefundAmount().compareTo(java.math.BigDecimal.ZERO) == 0) {
            return "Non-refundable. No refund due.";
        }
        if (payment.isFullyPaid() &&
                payment.getRefundAmount().compareTo(payment.getTotalAmount()) < 0) {
            // balance-only refund (deposit forfeited)
            return "Deposit forfeited. Refund: R" + payment.getRefundAmount();
        }
        return "Refund: R" + payment.getRefundAmount();
    }

    private void sendInvoiceEmail(Booking booking, Invoice invoice, String subject) {
        String userEmail = booking.getUser().getEmail();
        String body = buildEmailBody(booking, invoice);
        emailService.sendBookingConfirmation(userEmail, subject, body);
    }

    private String buildEmailBody(Booking booking, Invoice invoice) {
        return "Invoice: " + invoice.getInvoiceNumber() + "\n"
                + "Date: " + invoice.getInvoiceDate() + "\n"
                + "Booking ID: " + booking.getId() + "\n"
                + "Event Date: " + booking.getEventDate() + "\n"
                + "Total Amount: R" + invoice.getTotalAmount() + "\n"
                + "Deposit Amount: R" + invoice.getDepositAmount() + "\n"
                + "Balance Due: R" + invoice.getBalanceDue() + "\n"
                + (invoice.getRefundAmount() != null
                        ? "Refund Amount: R" + invoice.getRefundAmount() + "\n"
                        : "")
                + "Notes: " + invoice.getNotes();
    }

    /**
     * Enforces that the caller either owns the booking associated with an invoice
     * or holds the ADMIN role. Throws {@link BookingNotAllowedException} otherwise.
     */
    private void enforceOwnership(Booking booking, String userEmail) {
        if (booking == null) return;
        boolean isAdmin = userRepository.findByEmail(userEmail)
                .map(u -> u.getRole() == Role.ADMIN)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
        if (!isAdmin && !booking.getUser().getEmail().equals(userEmail)) {
            throw new BookingNotAllowedException("You are not authorised to view this invoice");
        }
    }
}
