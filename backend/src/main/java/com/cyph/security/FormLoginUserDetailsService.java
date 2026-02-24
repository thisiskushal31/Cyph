package com.cyph.security;

import com.cyph.config.CyphProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Loads a single admin user for form login when cyph.auth.form-login is configured.
 * Username should be one of cyph.auth.admin-emails so the user has admin access.
 */
@Service
@ConditionalOnProperty(prefix = "cyph.auth.form-login", name = "enabled", havingValue = "true")
public class FormLoginUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(FormLoginUserDetailsService.class);

    private final CyphProperties cyphProperties;

    public FormLoginUserDetailsService(CyphProperties cyphProperties) {
        this.cyphProperties = cyphProperties;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.info("Form login: loadUserByUsername called for username=[{}]", username != null ? username : "null");
        CyphProperties.Auth.FormLogin form = cyphProperties.getAuth().getFormLogin();
        String configUsername = form.getUsername();
        if (configUsername == null || configUsername.isBlank()) {
            log.warn("Form login: rejected - form login not configured (username blank)");
            throw new UsernameNotFoundException("Form login not configured");
        }
        // Fallback to "admin" when password is blank (e.g. ADMIN_PASSWORD="" or binding issue) so local dev works
        String password = (form.getPassword() != null && !form.getPassword().isBlank())
                ? form.getPassword() : "admin";
        if (!configUsername.equalsIgnoreCase(username == null ? "" : username.trim())) {
            log.warn("Form login: rejected - user not found (configUsername=[{}], requested=[{}])", configUsername, username);
            throw new UsernameNotFoundException("User not found");
        }
        log.info("Form login: user loaded for [{}], returning UserDetails", configUsername);
        return User.builder()
                .username(configUsername)
                .password("{noop}" + password)
                .roles("USER", "ADMIN")
                .build();
    }
}
