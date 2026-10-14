package codewithnathi.Catering.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {

    @NotNull
    private Long packageId;

    @NotNull
    @Future
    private LocalDate eventDate;

    @Min(1)
    private int numberOfGuests;

    private Double distanceKm;
}
