package com.cyph.security;

import com.cyph.config.CyphProperties;
import com.cyph.domain.AllowedUser;
import com.cyph.repository.AllowedUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Loads users for form login: first checks for an admin-added user in DB with a password;
 * otherwise falls back to the single config admin user when cyph.auth.form-login is configured.
 */
@Service
@ConditionalOnProperty(prefix = "cyph.auth.form-login", name = "enabled", havingValue = "true")
public class FormLoginUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(FormLoginUserDetailsService.class);

    private final CyphProperties cyphProperties;
    private final AllowedUserRepository allowedUserRepository;
    private final PasswordEncoder passwordEncoder;

    public FormLoginUserDetailsService(CyphProperties cyphProperties, AllowedUserRepository allowedUserRepository, PasswordEncoder passwordEncoder) {
        this.cyphProperties = cyphProperties;
        this.allowedUserRepository = allowedUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Form login: loadUserByUsername for username=[{}]", username != null ? username : "null");
        String trimmed = username == null ? "" : username.trim();
        // 1) Admin-added user in DB with password can sign in with form login
        var dbUser = allowedUserRepository.findByEmailIgnoreCase(trimmed)
                .filter(u -> u.getPasswordHash() != null && !u.getPasswordHash().isBlank());
        if (dbUser.isPresent()) {
            AllowedUser u = dbUser.get();
            boolean admin = cyphProperties.getAuth().getAdminEmails().stream().anyMatch(e -> e.equalsIgnoreCase(u.getEmail())) || u.isAdmin();
            List<String> roles = admin ? List.of("USER", "ADMIN") : List.of("USER");
            log.info("Form login: loaded DB user [{}]", u.getEmail());
            return User.builder()
                    .username(u.getEmail())
                    .password(u.getPasswordHash())
                    .roles(roles.toArray(new String[0]))
                    .build();
        }
        // 2) Config-based single super admin (from cyph.auth.form-login or env ADMIN_USERNAME/ADMIN_PASSWORD)
        CyphProperties.Auth.FormLogin form = cyphProperties.getAuth().getFormLogin();
        String configUsername = (form.getUsername() != null && !form.getUsername().isBlank())
                ? form.getUsername().trim() : "admin@localhost";
        String configPassword = (form.getPassword() != null && !form.getPassword().isBlank())
                ? form.getPassword() : "admin";
        if (!configUsername.equalsIgnoreCase(trimmed)) {
            log.warn("Form login: user not found (requested=[{}], config username=[{}])", trimmed, configUsername);
            throw new UsernameNotFoundException("User not found");
        }
        log.info("Form login: loaded config user [{}]", configUsername);
        return User.builder()
                .username(configUsername)
                .password(passwordEncoder.encode(configPassword))
                .roles("USER", "ADMIN")
                .build();
    }
}
