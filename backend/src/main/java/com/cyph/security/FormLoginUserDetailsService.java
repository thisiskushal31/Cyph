package com.cyph.security;

import com.cyph.domain.AllowedUser;
import com.cyph.repository.AllowedUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Loads users for form login from the database only. The super-admin user is seeded at startup
 * by SuperAdminSeeder from cyph.auth.form-login (ADMIN_USERNAME / ADMIN_PASSWORD), so no
 * config fallback is needed here.
 */
@Service
@ConditionalOnProperty(prefix = "cyph.auth.form-login", name = "enabled", havingValue = "true")
public class FormLoginUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(FormLoginUserDetailsService.class);

    private final AllowedUserRepository allowedUserRepository;

    public FormLoginUserDetailsService(AllowedUserRepository allowedUserRepository) {
        this.allowedUserRepository = allowedUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Form login: loadUserByUsername for username=[{}]", username != null ? username : "null");
        String trimmed = username == null ? "" : username.trim();
        var dbUser = allowedUserRepository.findByEmailIgnoreCase(trimmed)
                .filter(u -> u.getPasswordHash() != null && !u.getPasswordHash().isBlank());
        if (dbUser.isEmpty()) {
            log.warn("Form login: user not found for username=[{}]", trimmed);
            throw new UsernameNotFoundException("User not found");
        }
        AllowedUser u = dbUser.get();
        List<String> roles = u.isAdmin() ? List.of("USER", "ADMIN") : List.of("USER");
        log.info("Form login: loaded DB user [{}]", u.getEmail());
        return User.builder()
                .username(u.getEmail())
                .password(u.getPasswordHash())
                .roles(roles.toArray(new String[0]))
                .build();
    }
}
