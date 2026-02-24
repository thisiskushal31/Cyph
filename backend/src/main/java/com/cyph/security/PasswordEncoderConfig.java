package com.cyph.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Separate config for PasswordEncoder to avoid circular dependency:
 * AllowedUserService → SecurityConfig (for PasswordEncoder) → CyphOAuth2LoginSuccessHandler → AllowedUserService.
 */
@Configuration
public class PasswordEncoderConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
