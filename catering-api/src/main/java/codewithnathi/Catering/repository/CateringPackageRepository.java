package codewithnathi.Catering.repository;

import codewithnathi.Catering.entity.CateringPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CateringPackageRepository extends JpaRepository<CateringPackage, Long> {
    List<CateringPackage> findByIsActiveTrue();
}
