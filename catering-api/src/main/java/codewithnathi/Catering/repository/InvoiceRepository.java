package codewithnathi.Catering.repository;

import codewithnathi.Catering.entity.Invoice;
import codewithnathi.Catering.enums.InvoiceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data repository for {@link Invoice} entities.
 */
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

    /** Returns all invoices for a given booking. */
    List<Invoice> findByBooking_Id(Long bookingId);

    /** Returns invoices of a specific type for a given booking. */
    List<Invoice> findByBooking_IdAndInvoiceType(Long bookingId, InvoiceType type);
}
