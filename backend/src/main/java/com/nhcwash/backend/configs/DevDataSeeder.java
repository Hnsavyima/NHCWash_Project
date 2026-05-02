package com.nhcwash.backend.configs;

import java.math.BigDecimal;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.nhcwash.backend.models.constants.RoleNames;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import com.nhcwash.backend.models.entities.Role;
import com.nhcwash.backend.models.entities.Service;
import com.nhcwash.backend.models.entities.ServiceCategory;
import com.nhcwash.backend.models.entities.TimeSlot;
import com.nhcwash.backend.models.entities.User;
import com.nhcwash.backend.models.enumerations.SlotType;
import com.nhcwash.backend.repositories.RoleRepository;
import com.nhcwash.backend.repositories.ServiceCategoryRepository;
import com.nhcwash.backend.repositories.ServiceRepository;
import com.nhcwash.backend.repositories.TimeSlotRepository;
import com.nhcwash.backend.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Seeds catalogue services and staff test accounts when the DB has no services yet (idempotent for users by email).
 */
@Component
@Order(2)
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private static final String ADMIN_EMAIL = "admin@nhcwash.be";
    private static final String EMPLOYEE_EMAIL = "employee@nhcwash.be";

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ServiceRepository serviceRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final TimeSlotRepository timeSlotRepository;

    private static final ZoneId BRUSSELS = ZoneId.of("Europe/Brussels");

    @Override
    @Transactional
    public void run(String... args) {
        if (serviceRepository.count() == 0) {
            seedServices();
        }
        if (timeSlotRepository.count() == 0) {
            seedBusinessHourSlots();
        }
        ensureStaffUser(ADMIN_EMAIL, "Admin@123", "Admin", "NHCWash", RoleNames.ADMIN);
        ensureStaffUser(EMPLOYEE_EMAIL, "Employee@123", "Employee", "NHCWash", RoleNames.EMPLOYEE);
    }

    private void seedServices() {
        ServiceCategory category = new ServiceCategory();
        category.setName("Catalogue");
        category.setDescription("Services NHCWash (données de démonstration)");
        category = serviceCategoryRepository.save(category);

        addService(category, "Lavage & Repassage", "Lavage et repassage au kilo ou à la pièce.", bd("3.00"), 48);
        addService(category, "Nettoyage à sec", "Textiles délicats et costumes.", bd("8.00"), 72);
        addService(category, "Service Express", "Traitement prioritaire sous 24h.", bd("6.00"), 24);
        addService(category, "Couettes & Draps", "Grand linge de maison.", bd("15.00"), 96);
        addService(category, "Traitement spécial", "Taches tenaces et détachage professionnel.", bd("12.00"), 72);
        addService(category, "Retouches", "Ourlets, fermetures, petites réparations.", bd("5.00"), 48);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private void addService(ServiceCategory category, String name, String description, BigDecimal price, int delayHours) {
        Service s = new Service();
        s.setCategory(category);
        s.setName(name);
        s.setDescription(description);
        s.setBasePrice(price);
        s.setEstimatedDelayHours(delayHours);
        s.setIsActive(true);
        serviceRepository.save(s);
    }

    /** Weekday 2h windows: 09–11, 11–13, 14–16, 16–18 (matches frontend mock generator). */
    private void seedBusinessHourSlots() {
        LocalDate d = LocalDate.now(BRUSSELS).plusDays(1);
        int businessDays = 0;
        LocalTime[][] windows = new LocalTime[][] {
                { LocalTime.of(9, 0), LocalTime.of(11, 0) },
                { LocalTime.of(11, 0), LocalTime.of(13, 0) },
                { LocalTime.of(14, 0), LocalTime.of(16, 0) },
                { LocalTime.of(16, 0), LocalTime.of(18, 0) },
        };
        while (businessDays < 10) {
            DayOfWeek dow = d.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                for (LocalTime[] w : windows) {
                    saveSlot(SlotType.PICKUP, d, w[0], w[1]);
                    saveSlot(SlotType.DELIVERY, d, w[0], w[1]);
                }
                businessDays++;
            }
            d = d.plusDays(1);
        }
    }

    private void saveSlot(SlotType type, LocalDate day, LocalTime start, LocalTime end) {
        TimeSlot slot = new TimeSlot();
        slot.setSlotType(type);
        slot.setStartAt(LocalDateTime.of(day, start));
        slot.setEndAt(LocalDateTime.of(day, end));
        slot.setCapacityMax(8);
        slot.setIsActive(true);
        timeSlotRepository.save(slot);
    }

    private void ensureStaffUser(String email, String rawPassword, String firstName, String lastName, String roleName) {
        if (userRepository.existsByEmail(email)) {
            return;
        }
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalStateException("Role missing: " + roleName + " — run RolesInitializer first"));
        User user = new User();
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPhone(null);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setIsActive(true);
        user.setPreferredLanguage("FR");
        userRepository.save(user);
    }
}
