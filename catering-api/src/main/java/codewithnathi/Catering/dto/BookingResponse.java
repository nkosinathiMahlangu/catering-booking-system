package codewithnathi.Catering.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private Long id;
    private Long packageId;
    private String packageName;
    private LocalDate eventDate;
    private LocalDate bookingDate;
    private int numberOfGuests;
    private BigDecimal totalPrice;
    private BigDecimal transportCost;
    private Double distanceKm;
    private String status;
    private LocalDate cancellationDate;
    private LocalDateTime createdAt;
    private PaymentSummary payment;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentSummary {
        private boolean depositPaid;
        private boolean fullyPaid;
        private BigDecimal depositAmount;
        private BigDecimal balanceDue;
        private BigDecimal refundAmount;
    }
}
