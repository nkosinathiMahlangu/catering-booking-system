package codewithnathi.Catering.exception;

/**
 * Thrown when a cancellation is attempted within the non-refundable window
 * (≤ 4 days before the event date).
 * Fully implemented in FEAT-003.
 */
public class CancellationNotAllowedException extends RuntimeException {

    public CancellationNotAllowedException(String message) {
        super(message);
    }
}
