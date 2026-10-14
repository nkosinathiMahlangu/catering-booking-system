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
public class PaymentResponse {

    private Long id;
    private Long bookingId;
    private BigDecimal totalAmount;
    private BigDecimal depositAmount;
    private BigDecimal balanceDue;
    private LocalDate depositPaidDate;
    private LocalDate balancePaidDate;
    private boolean depositPaid;
    private boolean fullyPaid;
    private BigDecimal refundAmount;
    private LocalDateTime createdAt;
}
