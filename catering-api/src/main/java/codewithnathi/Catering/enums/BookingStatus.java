package codewithnathi.Catering.enums;

/**
 * Lifecycle states for a Booking.
 * PENDING   — created, awaiting deposit payment.
 * CONFIRMED — deposit or full payment received.
 * CANCELLED — cancelled by the user (refund rules apply).
 * COMPLETED — event has taken place.
 */
public enum BookingStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED
}
