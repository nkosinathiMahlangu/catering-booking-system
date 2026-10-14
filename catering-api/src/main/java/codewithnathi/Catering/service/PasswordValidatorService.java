package codewithnathi.Catering.service;

import org.springframework.stereotype.Service;

/**
 * Enforces password strength rules for registration and password reset.
 * <p>
 * Rules applied:
 * <ul>
 *   <li>Minimum 6 characters</li>
 *   <li>At least one uppercase letter</li>
 *   <li>At least one lowercase letter</li>
 *   <li>At least one digit</li>
 *   <li>At least one special character from: {@code !@#$%^&*()_+-=[]|,.?/:;<>{}}</li>
 *   <li>Must not contain the user's first or last name (case-insensitive)</li>
 * </ul>
 * </p>
 */
@Service
public class PasswordValidatorService {

    /**
     * Validates a password against all strength rules.
     *
     * @param password the plain-text password to validate
     * @param name     the user's first name (used to check for personal info in password)
     * @param surname  the user's last name (used to check for personal info in password)
     * @throws IllegalArgumentException with a descriptive message on the first rule that fails
     */
    public void validate(String password, String name, String surname) {
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        // Characters accepted as "special" — kept as a literal string for fast indexOf lookup
        String specials = "!@#$%^&*()_+-=[]|,.?/:;<>{}";

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c))      hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c))     hasDigit = true;
            else if (specials.indexOf(c) >= 0) hasSpecial = true;
        }

        if (!hasUpper) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter.");
        }
        if (!hasLower) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter.");
        }
        if (!hasDigit) {
            throw new IllegalArgumentException("Password must contain at least one digit.");
        }
        if (!hasSpecial) {
            throw new IllegalArgumentException(
                    "Password must contain at least one special character (!@#$%^&*()_+-=[]|,.?/:;<>{}).");
        }

        // Prevent passwords that include the user's own name — common credential-stuffing weakness
        String lowerPassword = password.toLowerCase();
        if (name != null && !name.isBlank() && lowerPassword.contains(name.toLowerCase())) {
            throw new IllegalArgumentException("Password must not contain your first name.");
        }
        if (surname != null && !surname.isBlank() && lowerPassword.contains(surname.toLowerCase())) {
            throw new IllegalArgumentException("Password must not contain your last name.");
        }
    }
}
