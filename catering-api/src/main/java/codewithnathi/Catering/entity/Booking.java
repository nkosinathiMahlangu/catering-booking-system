package codewithnathi.Catering.entity;

import codewithnathi.Catering.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA entity representing a catering booking made by a user.
 * Transport cost is added when the event is more than 15 km away.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "booking")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "package_id")
    private CateringPackage cateringPackage;

    private LocalDate eventDate;

    private LocalDate bookingDate;

    private int numberOfGuests;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalPrice;

    @Column(precision = 10, scale = 2)
    private BigDecimal transportCost;

    private Double distanceKm;

    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    private LocalDate cancellationDate;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
