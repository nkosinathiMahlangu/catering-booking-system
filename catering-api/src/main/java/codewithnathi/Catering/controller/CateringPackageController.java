package codewithnathi.Catering.controller;

import codewithnathi.Catering.dto.PackageRequest;
import codewithnathi.Catering.dto.PackageResponse;
import codewithnathi.Catering.service.CateringPackageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for catering package management.
 * <p>
 * Public endpoints (no JWT required):
 * <ul>
 *   <li>{@code GET /api/packages} — browse active packages</li>
 *   <li>{@code GET /api/packages/{id}} — view a single package</li>
 * </ul>
 * Admin-only endpoints (require ADMIN role):
 * <ul>
 *   <li>{@code GET /api/packages/admin/all} — view all packages including inactive</li>
 *   <li>{@code POST /api/packages} — create a new package</li>
 *   <li>{@code PUT /api/packages/{id}} — update an existing package</li>
 *   <li>{@code DELETE /api/packages/{id}} — soft-delete (deactivate) a package</li>
 * </ul>
 * </p>
 */
@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
public class CateringPackageController {

    private final CateringPackageService packageService;

    /**
     * Returns all active catering packages. No authentication required —
     * customers can browse the menu before signing up.
     *
     * @return 200 OK with a list of active {@link PackageResponse} objects
     */
    @GetMapping
    public ResponseEntity<List<PackageResponse>> getActivePackages() {
        return ResponseEntity.ok(packageService.getAllActivePackages());
    }

    /**
     * Returns a single package by id. No authentication required.
     *
     * @param id the package identifier
     * @return 200 OK with the {@link PackageResponse}, or 404 if not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<PackageResponse> getPackage(@PathVariable Long id) {
        return ResponseEntity.ok(packageService.getPackage(id));
    }

    /**
     * Returns all packages (active and inactive) for admin review.
     *
     * @return 200 OK with the full package catalogue
     */
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PackageResponse>> getAllPackages() {
        return ResponseEntity.ok(packageService.getAllPackages());
    }

    /**
     * Creates a new catering package.
     *
     * @param req validated package details
     * @return 201 Created with the saved {@link PackageResponse}
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PackageResponse> createPackage(@Valid @RequestBody PackageRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(packageService.createPackage(req));
    }

    /**
     * Updates all editable fields of an existing package.
     *
     * @param id  the id of the package to update
     * @param req the new field values
     * @return 200 OK with the updated {@link PackageResponse}, or 404 if not found
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PackageResponse> updatePackage(@PathVariable Long id,
                                                         @Valid @RequestBody PackageRequest req) {
        return ResponseEntity.ok(packageService.updatePackage(id, req));
    }

    /**
     * Soft-deletes a package by setting {@code isActive} to {@code false}.
     * The row is preserved in the database; it is simply hidden from public listings.
     *
     * @param id the id of the package to deactivate
     * @return 204 No Content on success, or 404 if not found
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivatePackage(@PathVariable Long id) {
        packageService.deactivatePackage(id);
        return ResponseEntity.noContent().build();
    }
}
