package codewithnathi.Catering.enums;

/**
 * Represents the three invoice types generated during a booking lifecycle.
 * BOOKING_CONFIRMATION — created when the deposit is paid.
 * PAYMENT_RECEIPT      — created when the full amount is paid.
 * CANCELLATION_STATEMENT — created when a booking is cancelled.
 */
public enum InvoiceType {
    BOOKING_CONFIRMATION,
    PAYMENT_RECEIPT,
    CANCELLATION_STATEMENT
}
