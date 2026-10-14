package codewithnathi.Catering.service;

import codewithnathi.Catering.dto.PaymentRequest;
import codewithnathi.Catering.dto.PaymentResponse;
import codewithnathi.Catering.entity.Booking;
import codewithnathi.Catering.entity.Payment;
import codewithnathi.Catering.entity.User;
import codewithnathi.Catering.enums.BookingStatus;
import codewithnathi.Catering.enums.Role;
import codewithnathi.Catering.exception.BookingNotAllowedException;
import codewithnathi.Catering.repository.BookingRepository;
import codewithnathi.Catering.repository.PaymentRepository;
import codewithnathi.Catering.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Handles deposit and full-payment processing for a booking.
 * <p>
 * On deposit payment: generates Invoice 1 (BOOKING_CONFIRMATION) and sets booking to CONFIRMED.
 * On full payment: generates Invoice 2 (PAYMENT_RECEIPT) and marks the booking fully paid.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final InvoiceService invoiceService;
    private final UserRepository userRepository;

    /**
     * Processes a payment (deposit or full) for a booking.
     *
     * @param req       payment request specifying bookingId and whether to pay in full
     * @param userEmail email of the authenticated user (must own the booking)
     * @return a {@link PaymentResponse} reflecting the updated payment state
     */
    public PaymentResponse processPayment(PaymentRequest req, String userEmail) {
        // 1. Load booking
        Booking booking = bookingRepository.findById(req.getBookingId())
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + req.getBookingId()));

        // 2. Verify ownership
        if (!booking.getUser().getEmail().equals(userEmail)) {
            throw new BookingNotAllowedException("You are not authorised to pay for this booking");
        }

        // 3. Load payment
        Payment payment = paymentRepository.findByBooking_Id(req.getBookingId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Payment record not found for booking: " + req.getBookingId()));

        if (req.isPayFull()) {
            // Full payment
            if (payment.isFullyPaid()) {
                throw new IllegalArgumentException("Booking is already fully paid");
            }
            payment.setFullyPaid(true);
            payment.setBalanceDue(java.math.BigDecimal.ZERO);
            payment.setBalancePaidDate(LocalDate.now());

            // If deposit was never separately paid, mark it now too
            if (!payment.isDepositPaid()) {
                payment.setDepositPaid(true);
                payment.setDepositPaidDate(LocalDate.now());
            }

            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            paymentRepository.save(payment);

            invoiceService.createPaymentReceipt(booking, payment);

        } else {
            // Deposit payment
            if (payment.isDepositPaid()) {
                throw new IllegalArgumentException("Deposit has already been paid");
            }
            payment.setDepositPaid(true);
            payment.setDepositPaidDate(LocalDate.now());
            payment.setBalanceDue(payment.getTotalAmount().subtract(payment.getDepositAmount()));

            booking.setStatus(BookingStatus.CONFIRMED);
            bookingRepository.save(booking);
            paymentRepository.save(payment);

            invoiceService.createBookingConfirmation(booking, payment);
        }

        return mapToResponse(payment, booking.getId());
    }

    /**
     * Returns the payment record for a booking.
     * Regular users may only view payments for their own bookings; admins can access any.
     *
     * @param bookingId the booking to look up
     * @param userEmail email of the authenticated user
     * @return the {@link PaymentResponse}
     */
    public PaymentResponse getPaymentForBooking(Long bookingId, String userEmail) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingId));

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isAdmin && !booking.getUser().getEmail().equals(userEmail)) {
            throw new BookingNotAllowedException("You are not authorised to view this payment");
        }

        Payment payment = paymentRepository.findByBooking_Id(bookingId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Payment record not found for booking: " + bookingId));

        return mapToResponse(payment, bookingId);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private PaymentResponse mapToResponse(Payment payment, Long bookingId) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .bookingId(bookingId)
                .totalAmount(payment.getTotalAmount())
                .depositAmount(payment.getDepositAmount())
                .balanceDue(payment.getBalanceDue())
                .depositPaidDate(payment.getDepositPaidDate())
                .balancePaidDate(payment.getBalancePaidDate())
                .depositPaid(payment.isDepositPaid())
                .fullyPaid(payment.isFullyPaid())
                .refundAmount(payment.getRefundAmount())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
