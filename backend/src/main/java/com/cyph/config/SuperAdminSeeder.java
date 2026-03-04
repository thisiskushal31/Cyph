package com.cyph.config;

import com.cyph.domain.AllowedUser;
import com.cyph.repository.AllowedUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Ensures the super-admin user from config (ADMIN_USERNAME / ADMIN_PASSWORD or cyph.auth.form-login)
 * exists in the database with the correct password hash. Login then checks the database only.
 * Runs once after the application is ready (e.g. after DB is available).
 */
@Component
@ConditionalOnProperty(prefix = "cyph.auth.form-login", name = "enabled", havingValue = "true")
public class SuperAdminSeeder {

    private static final Logger log = LoggerFactory.getLogger(SuperAdminSeeder.class);

    private final CyphProperties cyphProperties;
    private final AllowedUserRepository allowedUserRepository;
    private final PasswordEncoder passwordEncoder;

    public SuperAdminSeeder(CyphProperties cyphProperties,
                            AllowedUserRepository allowedUserRepository,
                            PasswordEncoder passwordEncoder) {
        this.cyphProperties = cyphProperties;
        this.allowedUserRepository = allowedUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(100)
    public void seedSuperAdmin() {
        CyphProperties.Auth.FormLogin form = cyphProperties.getAuth().getFormLogin();
        String username = form.getUsername() != null && !form.getUsername().isBlank()
                ? form.getUsername().trim()
                : "admin@localhost";
        String password = form.getPassword() != null && !form.getPassword().isBlank()
                ? form.getPassword()
                : "admin";

        allowedUserRepository.findByEmailIgnoreCase(username)
                .ifPresentOrElse(
                        existing -> {
                            existing.setPasswordHash(passwordEncoder.encode(password));
                            existing.setAdmin(true);
                            allowedUserRepository.save(existing);
                            log.info("Super-admin user updated in database: {}", username);
                        },
                        () -> {
                            AllowedUser superAdmin = new AllowedUser();
                            superAdmin.setEmail(username);
                            superAdmin.setSource(AllowedUser.Source.ADMIN_ADDED);
                            superAdmin.setAdmin(true);
                            superAdmin.setPasswordHash(passwordEncoder.encode(password));
                            allowedUserRepository.save(superAdmin);
                            log.info("Super-admin user created in database: {}", username);
                        }
                );
    }
}
