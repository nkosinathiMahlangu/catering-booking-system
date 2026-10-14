package codewithnathi.Catering.repository;

import codewithnathi.Catering.entity.Booking;
import codewithnathi.Catering.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data repository for {@link Booking} entities.
 */
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /** Returns all bookings belonging to a specific user. */
    List<Booking> findByUser_Id(Long userId);

    /**
     * Counts active bookings for a given event date (used to enforce the 3-booking cap).
     * Only PENDING and CONFIRMED bookings count toward the cap; CANCELLED ones are excluded.
     */
    long countByEventDateAndStatusIn(LocalDate eventDate, List<BookingStatus> statuses);
}
