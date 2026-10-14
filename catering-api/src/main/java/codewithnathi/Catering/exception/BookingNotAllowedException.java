package codewithnathi.Catering.exception;

/**
 * Thrown when a booking cannot be created due to business rule violations
 * (e.g. too close to event date, or event date already has 3 bookings).
 * Fully implemented in FEAT-003.
 */
public class BookingNotAllowedException extends RuntimeException {

    public BookingNotAllowedException(String message) {
        super(message);
    }
}
