package codewithnathi.Catering.service;

import codewithnathi.Catering.entity.Booking;
import codewithnathi.Catering.entity.Invoice;
import codewithnathi.Catering.entity.Payment;
import codewithnathi.Catering.enums.InvoiceType;
import codewithnathi.Catering.repository.InvoiceRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    /**
     * Creates a booking-confirmation invoice when the deposit is paid.
     * Sent to the user's registered email.
     *
     * @param booking the booking being confirmed
     * @param payment the associated payment record
     * @return the saved {@link Invoice}
     */
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
     *
     * @param booking the booking that has been fully paid
     * @param payment the associated payment record
     * @return the saved {@link Invoice}
     */
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
     *
     * @param booking the booking being cancelled
     * @param payment the associated payment record (must already have refundAmount set)
     * @return the saved {@link Invoice}
     */
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
     *
     * @param bookingId the booking id
     * @return list of invoices (may be empty)
     */
    public List<Invoice> getInvoicesForBooking(Long bookingId) {
        return invoiceRepository.findByBooking_Id(bookingId);
    }

    /**
     * Returns a single invoice by id.
     *
     * @param invoiceId the invoice id
     * @return the matching {@link Invoice}
     * @throws EntityNotFoundException if no invoice with that id exists
     */
    public Invoice getInvoice(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found with id: " + invoiceId));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

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
}
