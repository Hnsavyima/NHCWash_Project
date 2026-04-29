package com.nhcwash.backend.configs;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.nhcwash.backend.models.constants.RoleNames;
import com.nhcwash.backend.models.entities.Role;
import com.nhcwash.backend.repositories.RoleRepository;

import lombok.RequiredArgsConstructor;

/**
 * Ensures default roles exist. Without rows in {@code roles}, registration cannot link {@code ROLE_CLIENT}.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class RolesInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        ensureRole(RoleNames.CLIENT);
        ensureRole(RoleNames.ADMIN);
        ensureRole(RoleNames.EMPLOYEE);
    }

    private void ensureRole(String name) {
        if (roleRepository.findByName(name).isEmpty()) {
            Role role = new Role();
            role.setName(name);
            roleRepository.save(role);
        }
    }
}
