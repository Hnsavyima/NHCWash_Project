package com.nhcwash.backend.models.constants;

/**
 * Must match {@code roles.name} values and Spring Security {@code hasAuthority} checks (ROLE_* prefix).
 */
public final class RoleNames {

    private RoleNames() {}

    public static final String CLIENT = "ROLE_CLIENT";
    public static final String ADMIN = "ROLE_ADMIN";
    public static final String EMPLOYEE = "ROLE_EMPLOYEE";
}
