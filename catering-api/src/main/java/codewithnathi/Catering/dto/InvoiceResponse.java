package codewithnathi.Catering.dto;

import codewithnathi.Catering.enums.InvoiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Safe projection of an {@code Invoice} entity for API responses.
 * Flattens the Booking association to scalar fields so that no entity graph
 * (and in particular no User password hash) is ever serialized to JSON.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {

    private Long id;
    private String invoiceNumber;
    private InvoiceType invoiceType;
    private LocalDate invoiceDate;

    /** Flat booking fields — avoids serializing the full Booking → User graph. */
    private Long bookingId;
    private LocalDate eventDate;

    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
    private BigDecimal balanceDue;
    private BigDecimal refundAmount;
    private String notes;
    private LocalDateTime createdAt;
}
