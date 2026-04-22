package com.nhcwash.backend.models.constants;

/**
 * Shared password policy for registration and password reset (must match frontend).
 */
public final class PasswordRules {

    private PasswordRules() {
    }

    public static final String PATTERN = "^(?=.*[0-9])(?=.*[!@#$%^&*]).{8,}$";

    public static final String MESSAGE =
            "Le mot de passe doit comporter au moins 8 caractères, un chiffre et un caractère spécial parmi !@#$%^&*";
}
