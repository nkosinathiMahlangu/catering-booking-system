package codewithnathi.Catering.repository;

import codewithnathi.Catering.entity.Booking;
import codewithnathi.Catering.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findByUser_Id(Long userId);

    long countByEventDateAndStatusIn(LocalDate eventDate, List<BookingStatus> statuses);
}
