package codewithnathi.Catering.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageResponse {

    private Long id;
    private String packageName;
    private String mealTypes;
    private BigDecimal pricePerHead;
    private String description;
    private boolean isActive;
    private LocalDateTime createdAt;
}
