# Implementation Plan — Catering Booking System

Base package: `codewithnathi.Catering`  
Root: `src/main/java/codewithnathi/Catering/`  
Build: `.\mvnw.cmd compile` | Tests: `.\mvnw.cmd test`

---

## Step 1 — Foundation: dependencies, config, env safety, entity & repo fixes

### 1.1 — pom.xml: add JWT, validation, mail dependencies

Add inside `<dependencies>`:

```xml
<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.6</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.12.6</version>
    <scope>runtime</scope>
</dependency>
<!-- Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
<!-- Mail -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

Files: `pom.xml`  
Verify: `.\mvnw.cmd compile -q` — exits 0, no errors.

---

### 1.2 — application.yaml: env var placeholders + mail block

Replace the hardcoded datasource password and add JWT + mail configuration:

```yaml
spring:
  application:
    name: Catering
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/catering_db}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD}
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

jwt:
  secret: ${JWT_SECRET}
  expiration: ${JWT_EXPIRATION:86400000}

server:
  port: 8080
```

Files: `src/main/resources/application.yaml`  
Verify: File saved correctly; `.\mvnw.cmd compile -q` — exits 0.

---

### 1.3 — .gitignore: append sensitive config patterns

Append to existing `.gitignore`:

```
### Secrets / environment ###
.env
.env.*
application-local.yaml
application-local.yml
application-prod.yaml
application-prod.yml
*.p12
*.jks
secrets/
```

Files: `.gitignore`  
Verify: File contains the new entries; `.\mvnw.cmd compile -q` — exits 0.

---

### 1.4 — Payment.java: add refundAmount field

Add one field after `balanceDue`:

```java
@Column(precision = 10, scale = 2)
private BigDecimal refundAmount;
```

Files: `src/main/java/codewithnathi/Catering/entity/Payment.java`  
Verify: `.\mvnw.cmd compile -q` — exits 0, Payment class compiles.

---

### 1.5 — BookingRepository: add countByEventDateAndStatusIn

Add to the interface:

```java
import codewithnathi.Catering.entity.BookingStatus;
import java.time.LocalDate;
import java.util.List;

long countByEventDateAndStatusIn(LocalDate eventDate, List<BookingStatus> statuses);
```

Also add a convenience finder needed by BookingService:

```java
List<Booking> findByEventDate(LocalDate eventDate);
```

Files: `src/main/java/codewithnathi/Catering/repository/BookingRepository.java`  
Verify: `.\mvnw.cmd compile -q` — exits 0.

---

## Step 2 — Auth layer (depends on Step 1)

### 2.1 — JwtProperties configuration bean

Create `@ConfigurationProperties(prefix = "jwt")` record/class bound to the `jwt.secret` and `jwt.expiration` yaml keys. Needs `@EnableConfigurationProperties` on the main application class or `@Configuration` on this class.

File: `src/main/java/codewithnathi/Catering/config/JwtProperties.java`

```java
package codewithnathi.Catering.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private String secret;
    private long expiration; // milliseconds
}
```

Files: `src/main/java/codewithnathi/Catering/config/JwtProperties.java`  
Verify: `.\mvnw.cmd compile -q` — exits 0.

---

### 2.2 — JwtUtil

Token generation and validation using JJWT 0.12.x API (`Jwts.builder()`, `Jwts.parser()`). The utility injects `JwtProperties`.

Key methods:
- `String generateToken(UserDetails user)` — signs with HS256, sets subject=email, expiry from JwtProperties
- `String extractEmail(String token)` — returns subject claim
- `boolean isTokenValid(String token, UserDetails user)` — checks subject match + expiry

File: `src/main/java/codewithnathi/Catering/config/JwtUtil.java`

Note: Use JJWT 0.12.x API: `Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret))` and `Jwts.parser().verifyWith(key).build().parseSignedClaims(token)`.

Files: `src/main/java/codewithnathi/Catering/config/JwtUtil.java`  
Verify: `.\mvnw.cmd compile -q` — exits 0.

---

### 2.3 — DTOs (auth + shared)

Create the following in `dto` package. Use `@Data`, `@Builder`, Lombok. Use Jakarta Validation annotations (`@NotBlank`, `@Email`, `@Size`, etc.) on request DTOs.

**RegisterRequest.java**
Fields: `email` (`@Email @NotBlank`), `password` (`@NotBlank @Size(min=6)`), `name` (`@NotBlank`), `surname` (`@NotBlank`)

**LoginRequest.java**
Fields: `email` (`@Email @NotBlank`), `password` (`@NotBlank`)

**AuthResponse.java**
Fields: `token` (String), `email` (String), `role` (String), `name` (String)

**OtpRequest.java**
Fields: `email` (`@Email @NotBlank`), `otpCode` (`@NotBlank`)

**ResetPasswordRequest.java**
Fields: `email` (`@Email @NotBlank`), `otpCode` (`@NotBlank`), `newPassword` (`@NotBlank @Size(min=6)`)

**ForgotPasswordRequest.java**
Fields: `email` (`@Email @NotBlank`)

**PackageRequest.java** (admin creates/updates packages)
Fields: `packageName` (`@NotBlank`), `mealTypes` (`@NotBlank`), `pricePerHead` (`@NotNull @DecimalMin("0.01")`), `description`, `isActive` (boolean)

**PackageResponse.java**
Fields: `id`, `packageName`, `mealTypes`, `pricePerHead`, `description`, `isActive`, `createdAt`

**BookingRequest.java**
Fields: `packageId` (`@NotNull`), `eventDate` (`@NotNull @Future`), `numberOfGuests` (`@Min(1)`), `distanceKm` (Double)

**BookingResponse.java**
Fields: `id`, `packageName`, `eventDate`, `bookingDate`, `numberOfGuests`, `totalPrice`, `transportCost`, `distanceKm`, `status`, `cancellationDate`, `createdAt`, nested `PaymentSummary` (depositPaid, fullyPaid, depositAmount, balanceDue, refundAmount)

**PaymentRequest.java**
Fields: `bookingId` (`@NotNull`), `payFull` (boolean — true = full payment, false = deposit only)

**PaymentResponse.java**
Fields: `id`, `bookingId`, `totalAmount`, `depositAmount`, `balanceDue`, `depositPaidDate`, `balancePaidDate`, `depositPaid`, `fullyPaid`, `refundAmount`, `createdAt`

**CancellationResponse.java**
Fields: `bookingId`, `status`, `refundAmount`, `message`

Files: all in `src/main/java/codewithnathi/Catering/dto/`  
Verify: `.\mvnw.cmd compile -q` — exits 0.

---

### 2.4 — PasswordValidatorService

Validates password strength. Injected by UserService and AuthService.

Rules (all must pass):
1. Minimum 6 characters
2. At least one uppercase letter
3. At least one digit
4. At least one special character (`!@#$%^&*()_+-=[]|,.?/:;<>{}`)
5. At least one lowercase letter
6. Password must NOT contain the user's `name` or `surname` (case-insensitive)

Method signature:
```java
void validate(String password, String name, String surname);
// throws IllegalArgumentException with descriptive message on failure
```

File: `src/main/java/codewithnathi/Catering/service/PasswordValidatorService.java`  
Verify: `.\mvnw.cmd compile -q` — exits 0.

---

### 2.5 — EmailService

Sends emails via Spring `JavaMailSender`. Two methods:
- `void sendOtpEmail(String to, String otpCode)` — subject "Your OTP Code", body includes OTP and "expires in 2 minutes"
- `void sendBookingConfirmation(String to, String subject, String body)` — generic booking/invoice email

File: `src/main/java/codewithnathi/Catering/service/EmailService.java`  
Verify: `.\mvnw.cmd compile -q` — exits 0.

---

### 2.6 — UserService (implements UserDetailsService)

Depends on: `UserRepository`, `PasswordValidatorService`, `EmailService`, `PasswordEncoder`.

Methods:
- `UserDetails loadUserByUsername(String email)` — used by Spring Security
- `User register(RegisterRequest req)` — validates password (calls PasswordValidatorService), checks email uniqueness, encodes password, sets `role=USER`, `isActive=true`, saves
- `void initiatePasswordReset(String email)` — generates 6-digit OTP, hashes and stores in `resetOtpCode`, sets `otpExpiry = now() + 2 minutes`, sends email via EmailService
- `void resetPassword(ResetPasswordRequest req)` — validates OTP not expired, validates OTP match, validates new password strength, encodes and saves
- `User getCurrentUser(String email)` — finds by email or throws

File: `src/main/java/codewithnathi/Catering/service/UserService.java`  
Verify: `.\mvnw.cmd compile -q` — exits 0.

---

### 2.7 — JwtAuthenticationFilter

Extends `OncePerRequestFilter`. Reads `Authorization: Bearer <token>` header, validates token via `JwtUtil`, sets `UsernamePasswordAuthenticationToken` in `SecurityContextHolder` by loading the user from `UserService`.

File: `src/main/java/codewithnathi/Catering/config/JwtAuthenticationFilter.java`  
Verify: `.\mvnw.cmd compile -q` — exits 0.

---

### 2.8 — SecurityConfig

`@Configuration @EnableWebSecurity`. Depends on: `UserService`, `JwtUtil`, `JwtAuthenticationFilter`.

Rules:
- `BCryptPasswordEncoder` bean
- `AuthenticationManager` bean using `DaoAuthenticationProvider` with UserService + BCrypt
- Stateless session (STATELESS)
- CORS enabled (delegate to `CorsConfig`)
- CSRF disabled (stateless JWT API)
- Permit: `POST /api/auth/**` (public)
- Permit: `GET /api/packages` (public — browse packages)
- Require `ROLE_ADMIN`: `POST/PUT/DELETE /api/packages/**`, `GET /api/admin/**`
- Require `ROLE_USER` or `ROLE_ADMIN`: everything else under `/api/**`
- Add `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`

File: `src/main/java/codewithnathi/Catering/config/SecurityConfig.java`  
Verify: `.\mvnw.cmd compile -q` — exits 0.

---

### 2.9 — CorsConfig

`@Configuration`. Exposes a `CorsConfigurationSource` bean allowing:
- Origins: `${CORS_ORIGINS:http://localhost:3000}` (from env var)
- Methods: GET, POST, PUT, DELETE, OPTIONS
- Headers: `*`
- AllowCredentials: true

File: `src/main/java/codewithnathi/Catering/config/CorsConfig.java`  
Verify: `.\mvnw.cmd compile -q` — exits 0.

---

### 2.10 — AuthService

Depends on: `UserService`, `AuthenticationManager`, `JwtUtil`.

Methods:
- `AuthResponse register(RegisterRequest req)` — delegates to UserService.register, returns JWT
- `AuthResponse login(LoginRequest req)` — authenticates via AuthenticationManager, generates JWT
- `void forgotPassword(ForgotPasswordRequest req)` — delegates to UserService.initiatePasswordReset
- `void resetPassword(ResetPasswordRequest req)` — delegates to UserService.resetPassword
- `void verifyOtp(OtpRequest req)` — checks OTP validity (for standalone OTP check endpoint)

File: `src/main/java/codewithnathi/Catering/service/AuthService.java`  
Verify: `.\mvnw.cmd compile -q` — exits 0.

---

### 2.11 — AuthController

`@RestController @RequestMapping("/api/auth")`. No auth required (public).

| Method | Path | Body | Description |
|--------|------|------|-------------|
| POST | `/register` | `RegisterRequest` | Register new user |
| POST | `/login` | `LoginRequest` | Returns `AuthResponse` with JWT |
| POST | `/forgot-password` | `ForgotPasswordRequest` | Sends OTP to email |
| POST | `/verify-otp` | `OtpRequest` | Verifies OTP validity |
| POST | `/reset-password` | `ResetPasswordRequest` | Resets password after OTP |

File: `src/main/java/codewithnathi/Catering/controller/AuthController.java`  
Verify: `.\mvnw.cmd compile -q` — exits 0.

---

## Step 3 — Booking, Payment & Invoices (depends on Steps 1 & 2)

### 3.1 — InvoiceType enum

```java
public enum InvoiceType {
    BOOKING_CONFIRMATION,   // Invoice 1: generated when deposit paid
    PAYMENT_RECEIPT,        // Invoice 2: generated when full payment made
    CANCELLATION_STATEMENT  // Invoice 3: generated on cancellation
}
```

File: `src/main/java/codewithnathi/Catering/entity/InvoiceType.java`  
Verify: `.\mvnw.cmd compile -q` — exits 0.

---

### 3.2 — Invoice entity

```java
@Entity @Table(name = "invoice")
public class Invoice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "booking_id")
    private Booking booking;

    @Enumerated(EnumType.STRING)
    private InvoiceType invoiceType;

    private String invoiceNumber;      // e.g. "INV-2024-001"
    private LocalDate invoiceDate;

    @Column(precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal depositAmount;

    @Column(precision = 10, scale = 2)
    private BigDecimal balanceDue;

    @Column(precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(columnDefinition = "TEXT")
    private String notes;              // e.g. "Non-refundable deposit" or "Refund: R500.00"

    @CreationTimestamp @Column(updatable = false)
    private LocalDateTime createdAt;
}
```

File: `src/main/java/codewithnathi/Catering/entity/Invoice.java`  
Verify: `.\mvnw.cmd compile -q` — exits 0.

---

### 3.3 — InvoiceRepository

```java
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByBooking_Id(Long bookingId);
    List<Invoice> findByBooking_IdAndInvoiceType(Long bookingId, InvoiceType type);
}
```

File: `src/main/java/codewithnathi/Catering/repository/InvoiceRepository.java`  
Verify: `.\mvnw.cmd compile -q` — exits 0.

---

### 3.4 — InvoiceService

Depends on: `InvoiceRepository`, `EmailService`.

Methods:
- `Invoice createBookingConfirmation(Booking booking, Payment payment)` — Invoice 1, sent when deposit paid
- `Invoice createPaymentReceipt(Booking booking, Payment payment)` — Invoice 2, sent when fully paid
- `Invoice createCancellationStatement(Booking booking, Payment payment)` — Invoice 3, shows forfeited/refunded amounts
- `List<Invoice> getInvoicesForBooking(Long bookingId)` — returns all invoices for a booking
- `Invoice getInvoice(Long invoiceId)` — or throws

Invoice number generation: `"INV-" + year + "-" + String.format("%05d", id)` — set after save using a second save or use a DB sequence.

Each create method also calls `EmailService.sendBookingConfirmation(...)` with a formatted text summary.

File: `src/main/java/codewithnathi/Catering/service/InvoiceService.java`  
Verify: `.\mvnw.cmd compile -q` — exits 0.

---

### 3.5 — Cancellation logic (CancellationNotAllowedException, BookingNotAllowedException)

**BookingNotAllowedException** — thrown when:
- `eventDate` is within 5 days of `bookingDate` (must be at least 5 days apart)
- 3 or more bookings already exist for the requested `eventDate` with status in `[PENDING, CONFIRMED]`

**CancellationNotAllowedException** — thrown when:
- `eventDate` is within 4 days (≤ 3 days gap) AND user attempts to cancel expecting a full refund

Both extend `RuntimeException`. A `GlobalExceptionHandler` (`@RestControllerAdvice`) catches them and returns structured error JSON `{ "error": "...", "message": "..." }` with HTTP 400 or 409.

Also handle: `EntityNotFoundException` → 404, `IllegalArgumentException` → 400, `org.springframework.security.access.AccessDeniedException` → 403, general `Exception` → 500.

Files:
- `src/main/java/codewithnathi/Catering/exception/BookingNotAllowedException.java`
- `src/main/java/codewithnathi/Catering/exception/CancellationNotAllowedException.java`
- `src/main/java/codewithnathi/Catering/exception/GlobalExceptionHandler.java`

Verify: `.\mvnw.cmd compile -q` — exits 0.

---

### 3.6 — BookingService

Depends on: `BookingRepository`, `CateringPackageRepository`, `PaymentRepository`, `InvoiceService`, `EmailService`, `UserRepository`.

**Business rules enforced here:**

**Booking creation** (`BookingResponse createBooking(BookingRequest req, String userEmail)`):
1. Load user by email
2. Load CateringPackage; check `isActive = true`
3. Check `eventDate - bookingDate >= 5 days`; throw `BookingNotAllowedException` if not
4. Check `countByEventDateAndStatusIn(eventDate, [PENDING, CONFIRMED]) < 3`; throw `BookingNotAllowedException("Event date is fully booked")` if not
5. Calculate `totalPrice = pricePerHead × numberOfGuests`
6. Transport: if `distanceKm > 15`, `transportCost = (distanceKm - 15) × 25`; else `transportCost = 0`
7. `totalPrice += transportCost`
8. Create Booking with `status = PENDING`, `bookingDate = LocalDate.now()`
9. Create Payment: `totalAmount = totalPrice`, `depositAmount = totalPrice / 2`, `balanceDue = totalPrice`, `depositPaid = false`, `fullyPaid = false`, `refundAmount = 0`
10. Save both, return BookingResponse

**Booking cancellation** (`CancellationResponse cancelBooking(Long bookingId, String userEmail)`):

Apply the refund matrix exactly:

| Condition | Deposit paid | Fully paid | Refund |
|-----------|-------------|-----------|--------|
| > 4 days to event, deposit only | Refunded | N/A | Full deposit back |
| > 4 days to event, fully paid | Refunded | Refunded | Full amount back |
| ≤ 3 days to event, deposit only | Forfeited | N/A | R0 |
| ≤ 3 days to event, fully paid | Forfeited | Refunded | Balance only = totalAmount - depositAmount |

Steps:
1. Load booking, verify it belongs to the requesting user (or user has ROLE_ADMIN)
2. Check status is PENDING or CONFIRMED; throw if already CANCELLED/COMPLETED
3. Calculate `daysToEvent = ChronoUnit.DAYS.between(LocalDate.now(), eventDate)`
4. If `daysToEvent <= 3` AND `!payment.isDepositPaid() && !payment.isFullyPaid()` → throw `CancellationNotAllowedException` (nothing paid, nothing to cancel — just block it)
5. If `daysToEvent <= 3` → show "non-refundable" message; apply matrix
6. Compute `refundAmount` from matrix; set on payment
7. Set `booking.status = CANCELLED`, `booking.cancellationDate = LocalDate.now()`
8. Save booking, save payment
9. Call `InvoiceService.createCancellationStatement(...)`
10. Return `CancellationResponse` with refund info and appropriate message

**Other methods:**
- `BookingResponse getBooking(Long id, String userEmail)` — user can see own bookings; admin sees all
- `List<BookingResponse> getUserBookings(String userEmail)` — all bookings for current user
- `List<BookingResponse> getAllBookings()` — admin only (call from controller with security annotation)

File: `src/main/java/codewithnathi/Catering/service/BookingService.java`  
Verify: `.\mvnw.cmd compile -q` — exits 0.

---

### 3.7 — PaymentService

Depends on: `PaymentRepository`, `BookingRepository`, `InvoiceService`, `EmailService`.

Methods:

**`PaymentResponse processPayment(PaymentRequest req, String userEmail)`**
1. Load booking; verify it belongs to user
2. Load payment for booking
3. If `payFull = true`:
   - Check payment is not already fully paid
   - Set `fullyPaid = true`, `balanceDue = 0`, `balancePaidDate = today`
   - If deposit wasn't previously paid, set `depositPaid = true`, `depositPaidDate = today`
   - Call `InvoiceService.createPaymentReceipt(...)`
4. If `payFull = false` (deposit only):
   - Check deposit not already paid
   - Set `depositPaid = true`, `depositPaidDate = today`
   - Set `balanceDue = totalAmount - depositAmount`
   - Call `InvoiceService.createBookingConfirmation(...)`
5. Update `booking.status = CONFIRMED`
6. Save payment, save booking, return `PaymentResponse`

**`PaymentResponse getPaymentForBooking(Long bookingId, String userEmail)`** — returns payment details including refund display

File: `src/main/java/codewithnathi/Catering/service/PaymentService.java`  
Verify: `.\mvnw.cmd compile -q` — exits 0.

---

### 3.8 — BookingController

`@RestController @RequestMapping("/api/bookings")`. Requires authentication.

| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | `/` | USER | Create booking |
| GET | `/` | USER | Get own bookings |
| GET | `/{id}` | USER/ADMIN | Get single booking |
| POST | `/{id}/cancel` | USER/ADMIN | Cancel booking |
| GET | `/admin/all` | ADMIN | All bookings |

Extract `userEmail` from `SecurityContextHolder.getContext().getAuthentication().getName()`.

File: `src/main/java/codewithnathi/Catering/controller/BookingController.java`  
Verify: `.\mvnw.cmd compile -q` — exits 0.

---

### 3.9 — PaymentController

`@RestController @RequestMapping("/api/payments")`. Requires authentication.

| Method | Path | Role | Description |
|--------|------|------|-------------|
| POST | `/` | USER | Process payment (deposit or full) |
| GET | `/booking/{bookingId}` | USER/ADMIN | Get payment for a booking |

File: `src/main/java/codewithnathi/Catering/controller/PaymentController.java`  
Verify: `.\mvnw.cmd compile -q` — exits 0.

---

### 3.10 — InvoiceController

`@RestController @RequestMapping("/api/invoices")`. Requires authentication.

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/booking/{bookingId}` | USER/ADMIN | All invoices for a booking |
| GET | `/{invoiceId}` | USER/ADMIN | Get single invoice |

File: `src/main/java/codewithnathi/Catering/controller/InvoiceController.java`  
Verify: `.\mvnw.cmd compile -q` — exits 0.

---

## Step 4 — Catering Package management (depends on Step 2)

### 4.1 — CateringPackageService

Depends on: `CateringPackageRepository`.

Methods:
- `List<PackageResponse> getAllActivePackages()` — public, uses `findByIsActiveTrue()`
- `List<PackageResponse> getAllPackages()` — admin only, uses `findAll()`
- `PackageResponse getPackage(Long id)` — by ID
- `PackageResponse createPackage(PackageRequest req)` — admin creates new package
- `PackageResponse updatePackage(Long id, PackageRequest req)` — admin updates
- `void deactivatePackage(Long id)` — soft-delete: sets `isActive = false`

File: `src/main/java/codewithnathi/Catering/service/CateringPackageService.java`  
Verify: `.\mvnw.cmd compile -q` — exits 0.

---

### 4.2 — CateringPackageController

`@RestController @RequestMapping("/api/packages")`.

| Method | Path | Role | Description |
|--------|------|------|-------------|
| GET | `/` | PUBLIC | All active packages |
| GET | `/{id}` | PUBLIC | Single package |
| POST | `/` | ADMIN | Create package |
| PUT | `/{id}` | ADMIN | Update package |
| DELETE | `/{id}` | ADMIN | Deactivate package |
| GET | `/admin/all` | ADMIN | All packages (incl inactive) |

File: `src/main/java/codewithnathi/Catering/controller/CateringPackageController.java`  
Verify: `.\mvnw.cmd compile -q` — exits 0.

---

## Final verification

After all steps complete, run the full build:

```
.\mvnw.cmd test -Dspring.datasource.url=... 
```

Or compile-only to confirm no errors:

```
.\mvnw.cmd compile
```

Integration tests require a running PostgreSQL instance; compile-only verification is acceptable in CI without a DB.

---

## Dependency order summary

```
Step 1 (Foundation)
  └─> Step 2 (Auth): JwtProperties → JwtUtil → DTOs → PasswordValidatorService 
                     → EmailService → UserService → JwtAuthenticationFilter 
                     → SecurityConfig + CorsConfig → AuthService → AuthController
  └─> Step 3 (Bookings/Payments): InvoiceType → Invoice → InvoiceRepository 
                     → InvoiceService → Exceptions → BookingService → PaymentService 
                     → BookingController → PaymentController → InvoiceController
  └─> Step 4 (Packages): CateringPackageService → CateringPackageController
```

Steps 2, 3, and 4 are independent of each other (all depend only on Step 1).
Within Step 2, SecurityConfig must come after UserService (circular risk: SecurityConfig injects UserService which injects PasswordEncoder — use `@Lazy` on the PasswordEncoder injection in UserService, or define PasswordEncoder bean in a separate `@Configuration` class like `AppConfig.java` rather than inside SecurityConfig to break the cycle).
