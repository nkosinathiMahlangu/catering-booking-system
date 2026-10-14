package codewithnathi.Catering.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "booking_id")
    private Booking booking;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal depositAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal balanceDue;

    @Column(precision = 10, scale = 2)
    private BigDecimal refundAmount;

    private LocalDate depositPaidDate;

    private LocalDate balancePaidDate;

    private boolean depositPaid;

    private boolean fullyPaid;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
