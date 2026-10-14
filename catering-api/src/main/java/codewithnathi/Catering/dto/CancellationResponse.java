package codewithnathi.Catering.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancellationResponse {

    private Long bookingId;
    private String status;
    private BigDecimal refundAmount;
    private String message;
}
