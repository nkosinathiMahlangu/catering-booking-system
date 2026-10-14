package codewithnathi.Catering.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackageRequest {

    @NotBlank
    private String packageName;

    @NotBlank
    private String mealTypes;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal pricePerHead;

    private String description;

    private boolean isActive;
}
