package codewithnathi.Catering.service;

import codewithnathi.Catering.dto.BookingRequest;
import codewithnathi.Catering.dto.BookingResponse;
import codewithnathi.Catering.dto.CancellationResponse;
import codewithnathi.Catering.entity.Booking;
import codewithnathi.Catering.entity.CateringPackage;
import codewithnathi.Catering.entity.Payment;
import codewithnathi.Catering.entity.User;
import codewithnathi.Catering.enums.BookingStatus;
import codewithnathi.Catering.enums.Role;
import codewithnathi.Catering.exception.BookingNotAllowedException;
import codewithnathi.Catering.exception.CancellationNotAllowedException;
import codewithnathi.Catering.repository.BookingRepository;
import codewithnathi.Catering.repository.CateringPackageRepository;
import codewithnathi.Catering.repository.PaymentRepository;
import codewithnathi.Catering.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Core booking service that enforces all business rules for booking creation
 * and cancellation, including the refund matrix.
 */
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final CateringPackageRepository cateringPackageRepository;
    private final PaymentRepository paymentRepository;
    private final InvoiceService invoiceService;
    private final EmailService emailService;
    private final UserRepository userRepository;

    /**
     * Creates a new booking for the authenticated user.
     * <p>
     * Business rules enforced:
     * <ol>
     *   <li>Package must be active.</li>
     *   <li>Event date must be at least 5 days from today.</li>
     *   <li>No more than 3 PENDING/CONFIRMED bookings on the same event date.</li>
     *   <li>Transport cost of R25/km is added for every km beyond 15 km.</li>
     * </ol>
     * </p>
     *
     * @param req       booking request payload
     * @param userEmail email of the authenticated user (from JWT)
     * @return a {@link BookingResponse} with payment summary
     */
    public BookingResponse createBooking(BookingRequest req, String userEmail) {
        // 1. Load user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));

        // 2. Load and validate package
        CateringPackage pkg = cateringPackageRepository.findById(req.getPackageId())
                .orElseThrow(() -> new EntityNotFoundException("Package not found: " + req.getPackageId()));
        if (!pkg.isActive()) {
            throw new BookingNotAllowedException("Package not available");
        }

        // 3. Event date must be at least 5 days from today
        long daysUntilEvent = ChronoUnit.DAYS.between(LocalDate.now(), req.getEventDate());
        if (daysUntilEvent < 5) {
            throw new BookingNotAllowedException("Event date must be at least 5 days from today");
        }

        // 4. No more than 3 active bookings per event date
        long activeBookings = bookingRepository.countByEventDateAndStatusIn(
                req.getEventDate(), List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));
        if (activeBookings >= 3) {
            throw new BookingNotAllowedException("Event date is fully booked");
        }

        // 5. Calculate total price (pricePerHead × guests)
        BigDecimal totalPrice = pkg.getPricePerHead()
                .multiply(BigDecimal.valueOf(req.getNumberOfGuests()));

        // 6. Calculate transport cost: R25/km for every km beyond 15
        double distanceKm = req.getDistanceKm() != null ? req.getDistanceKm() : 0.0;
        BigDecimal transportCost = BigDecimal.ZERO;
        if (distanceKm > 15) {
            transportCost = BigDecimal.valueOf((distanceKm - 15) * 25);
        }
        totalPrice = totalPrice.add(transportCost);

        // 7. Save booking
        Booking booking = Booking.builder()
                .user(user)
                .cateringPackage(pkg)
                .eventDate(req.getEventDate())
                .bookingDate(LocalDate.now())
                .numberOfGuests(req.getNumberOfGuests())
                .totalPrice(totalPrice)
                .transportCost(transportCost)
                .distanceKm(distanceKm)
                .status(BookingStatus.PENDING)
                .build();
        booking = bookingRepository.save(booking);

        // 8. Save initial payment record (nothing paid yet)
        BigDecimal depositAmount = totalPrice.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);
        Payment payment = Payment.builder()
                .booking(booking)
                .totalAmount(totalPrice)
                .depositAmount(depositAmount)
                .balanceDue(totalPrice)
                .depositPaid(false)
                .fullyPaid(false)
                .refundAmount(BigDecimal.ZERO)
                .build();
        paymentRepository.save(payment);

        // 9. Return response
        return mapToResponse(booking, payment);
    }

    /**
     * Cancels a booking and applies the refund matrix based on days to event and payment state.
     *
     * <pre>
     * Refund matrix:
     *   daysToEvent > 4, no payment         → throw CancellationNotAllowedException
     *   daysToEvent > 4, deposit only        → refund = depositAmount
     *   daysToEvent > 4, fully paid          → refund = totalAmount
     *   daysToEvent ≤ 4, deposit only        → refund = ZERO (deposit forfeited)
     *   daysToEvent ≤ 4, fully paid          → refund = totalAmount - depositAmount (balance only)
     * </pre>
     *
     * @param bookingId booking to cancel
     * @param userEmail email of the authenticated user
     * @return a {@link CancellationResponse} with refund details
     */
    public CancellationResponse cancelBooking(Long bookingId, String userEmail) {
        // 1. Load booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + bookingId));

        // 2. Verify ownership or admin
        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;
        if (!booking.getUser().getEmail().equals(userEmail) && !isAdmin) {
            throw new BookingNotAllowedException("You are not authorised to cancel this booking");
        }

        // 3. Only PENDING or CONFIRMED bookings can be cancelled
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new CancellationNotAllowedException("Booking is already cancelled");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new CancellationNotAllowedException("Completed bookings cannot be cancelled");
        }

        // 4. Load payment
        Payment payment = paymentRepository.findByBooking_Id(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Payment record not found for booking: " + bookingId));

        // 5. Days to event
        long daysToEvent = ChronoUnit.DAYS.between(LocalDate.now(), booking.getEventDate());

        // 6. Apply refund matrix
        BigDecimal refund;
        String message;

        if (daysToEvent > 4 && !payment.isDepositPaid() && !payment.isFullyPaid()) {
            throw new CancellationNotAllowedException("Nothing to cancel — no payment made");
        } else if (daysToEvent > 4 && payment.isDepositPaid() && !payment.isFullyPaid()) {
            refund = payment.getDepositAmount();
            message = "Full deposit refunded";
        } else if (daysToEvent > 4 && payment.isFullyPaid()) {
            refund = payment.getTotalAmount();
            message = "Full amount refunded";
        } else if (daysToEvent <= 4 && payment.isDepositPaid() && !payment.isFullyPaid()) {
            refund = BigDecimal.ZERO;
            message = "Cancellation within 4 days of event. Deposit is non-refundable.";
        } else if (daysToEvent <= 4 && payment.isFullyPaid()) {
            refund = payment.getTotalAmount().subtract(payment.getDepositAmount());
            message = "Cancellation within 4 days. Deposit forfeited. Balance refunded.";
        } else {
            // No payment made and ≤ 4 days — allow cancellation with zero refund so the
            // booking slot is freed for other customers rather than held as a ghost booking.
            refund = BigDecimal.ZERO;
            message = "Booking cancelled. No payment was made — no refund applicable.";
        }

        // 7. Update payment and booking
        payment.setRefundAmount(refund);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationDate(LocalDate.now());

        // 8. Persist
        paymentRepository.save(payment);
        bookingRepository.save(booking);

        // 9. Generate cancellation invoice
        invoiceService.createCancellationStatement(booking, payment);

        // 10. Return response
        return CancellationResponse.builder()
                .bookingId(bookingId)
                .status(BookingStatus.CANCELLED.name())
                .refundAmount(refund)
                .message(message)
                .build();
    }

    /**
     * Returns a single booking. Regular users can only see their own bookings.
     *
     * @param id        the booking id
     * @param userEmail email of the authenticated user
     * @return the booking response
     */
    public BookingResponse getBooking(Long id, String userEmail) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Booking not found: " + id));

        User currentUser = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!booking.getUser().getEmail().equals(userEmail) && !isAdmin) {
            throw new BookingNotAllowedException("You are not authorised to view this booking");
        }

        Payment payment = paymentRepository.findByBooking_Id(id).orElse(null);
        return mapToResponse(booking, payment);
    }

    /**
     * Returns all bookings belonging to the authenticated user.
     *
     * @param userEmail email of the authenticated user
     * @return list of booking responses
     */
    public List<BookingResponse> getUserBookings(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + userEmail));

        return bookingRepository.findByUser_Id(user.getId()).stream()
                .map(booking -> {
                    Payment payment = paymentRepository.findByBooking_Id(booking.getId()).orElse(null);
                    return mapToResponse(booking, payment);
                })
                .collect(Collectors.toList());
    }

    /**
     * Returns all bookings (admin only — access enforced at controller level via @PreAuthorize).
     *
     * @return list of all booking responses
     */
    public List<BookingResponse> getAllBookings() {
        return bookingRepository.findAll().stream()
                .map(booking -> {
                    Payment payment = paymentRepository.findByBooking_Id(booking.getId()).orElse(null);
                    return mapToResponse(booking, payment);
                })
                .collect(Collectors.toList());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private BookingResponse mapToResponse(Booking booking, Payment payment) {
        BookingResponse.PaymentSummary paymentSummary = null;
        if (payment != null) {
            paymentSummary = BookingResponse.PaymentSummary.builder()
                    .depositPaid(payment.isDepositPaid())
                    .fullyPaid(payment.isFullyPaid())
                    .depositAmount(payment.getDepositAmount())
                    .balanceDue(payment.getBalanceDue())
                    .refundAmount(payment.getRefundAmount())
                    .build();
        }

        return BookingResponse.builder()
                .id(booking.getId())
                .packageId(booking.getCateringPackage().getId())
                .packageName(booking.getCateringPackage().getPackageName())
                .eventDate(booking.getEventDate())
                .bookingDate(booking.getBookingDate())
                .numberOfGuests(booking.getNumberOfGuests())
                .totalPrice(booking.getTotalPrice())
                .transportCost(booking.getTransportCost())
                .distanceKm(booking.getDistanceKm())
                .status(booking.getStatus().name())
                .cancellationDate(booking.getCancellationDate())
                .createdAt(booking.getCreatedAt())
                .payment(paymentSummary)
                .build();
    }
}
