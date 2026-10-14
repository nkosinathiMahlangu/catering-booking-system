package codewithnathi.Catering.service;

import codewithnathi.Catering.dto.PackageRequest;
import codewithnathi.Catering.dto.PackageResponse;
import codewithnathi.Catering.entity.CateringPackage;
import codewithnathi.Catering.repository.CateringPackageRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for managing catering packages.
 * <p>
 * Admins can create, update, and deactivate packages.
 * All users (including unauthenticated) can browse active packages.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class CateringPackageService {

    private final CateringPackageRepository cateringPackageRepository;

    /**
     * Returns all packages that are currently active (available for booking).
     * This is the public-facing listing used by unauthenticated customers.
     *
     * @return list of active {@link PackageResponse} objects
     */
    public List<PackageResponse> getAllActivePackages() {
        return cateringPackageRepository.findByIsActiveTrue()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Returns all packages regardless of active status.
     * Intended for admin use to review and manage the full catalogue.
     *
     * @return list of all {@link PackageResponse} objects
     */
    public List<PackageResponse> getAllPackages() {
        return cateringPackageRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Fetches a single package by its ID.
     *
     * @param id the package identifier
     * @return the corresponding {@link PackageResponse}
     * @throws EntityNotFoundException if no package exists with the given id (handled as 404)
     */
    public PackageResponse getPackage(Long id) {
        CateringPackage pkg = cateringPackageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Catering package not found with id: " + id));
        return mapToResponse(pkg);
    }

    /**
     * Creates a new catering package from the provided request payload.
     *
     * @param req the package details supplied by an admin
     * @return the saved package as a {@link PackageResponse}
     */
    public PackageResponse createPackage(PackageRequest req) {
        CateringPackage pkg = CateringPackage.builder()
                .packageName(req.getPackageName())
                .mealTypes(req.getMealTypes())
                .pricePerHead(req.getPricePerHead())
                .description(req.getDescription())
                .isActive(req.isActive())
                .build();
        return mapToResponse(cateringPackageRepository.save(pkg));
    }

    /**
     * Updates an existing package's editable fields.
     * All fields from the request are applied; the package's id and createdAt are preserved.
     *
     * @param id  the id of the package to update
     * @param req the new field values
     * @return the updated package as a {@link PackageResponse}
     * @throws EntityNotFoundException if no package exists with the given id
     */
    public PackageResponse updatePackage(Long id, PackageRequest req) {
        CateringPackage pkg = cateringPackageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Catering package not found with id: " + id));

        pkg.setPackageName(req.getPackageName());
        pkg.setMealTypes(req.getMealTypes());
        pkg.setPricePerHead(req.getPricePerHead());
        pkg.setDescription(req.getDescription());
        pkg.setActive(req.isActive());

        return mapToResponse(cateringPackageRepository.save(pkg));
    }

    /**
     * Soft-deletes a package by setting {@code isActive} to {@code false}.
     * The row is retained in the database; it simply no longer appears in public listings.
     *
     * @param id the id of the package to deactivate
     * @throws EntityNotFoundException if no package exists with the given id
     */
    public void deactivatePackage(Long id) {
        CateringPackage pkg = cateringPackageRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Catering package not found with id: " + id));
        pkg.setActive(false);
        cateringPackageRepository.save(pkg);
    }

    /**
     * Maps a {@link CateringPackage} entity to its {@link PackageResponse} DTO.
     *
     * @param pkg the entity to convert
     * @return the DTO representation
     */
    private PackageResponse mapToResponse(CateringPackage pkg) {
        return PackageResponse.builder()
                .id(pkg.getId())
                .packageName(pkg.getPackageName())
                .mealTypes(pkg.getMealTypes())
                .pricePerHead(pkg.getPricePerHead())
                .description(pkg.getDescription())
                .isActive(pkg.isActive())
                .createdAt(pkg.getCreatedAt())
                .build();
    }
}
