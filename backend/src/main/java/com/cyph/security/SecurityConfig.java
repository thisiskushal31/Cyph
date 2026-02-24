package com.cyph.security;

import com.cyph.config.CyphProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import java.util.Optional;

/**
 * Security configuration. Supports OAuth2 (SSO, Google) and/or form login (admin username/password).
 * Login page is the frontend at siteUrl/login; user chooses SSO, Google, or admin form.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CyphProperties cyphProperties;
    private final Optional<CyphOAuth2LoginSuccessHandler> successHandler;
    private final Optional<ClientRegistrationRepository> clientRegistrationRepository;
    private final Optional<UserDetailsService> userDetailsService;

    public SecurityConfig(CyphProperties cyphProperties,
                          Optional<CyphOAuth2LoginSuccessHandler> successHandler,
                          Optional<ClientRegistrationRepository> clientRegistrationRepository,
                          Optional<UserDetailsService> userDetailsService) {
        this.cyphProperties = cyphProperties;
        this.successHandler = successHandler;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/login"));

        String loginPage = cyphProperties.getSiteUrl() + "/login";
        boolean oauth2Enabled = clientRegistrationRepository.isPresent();
        boolean formLoginEnabled = userDetailsService.isPresent();
        boolean anyAuthEnabled = (oauth2Enabled && successHandler.isPresent()) || formLoginEnabled;

        http.authorizeHttpRequests(auth -> {
            auth.requestMatchers("/api/public/**", "/actuator/health", "/error", "/login", "/logout", "/oauth2/**").permitAll();
            if (anyAuthEnabled) {
                auth.anyRequest().authenticated();
            } else {
                auth.anyRequest().permitAll();
            }
        });

        if (oauth2Enabled && successHandler.isPresent()) {
            http.oauth2Login(oauth2 -> oauth2
                    .loginPage(loginPage)
                    .successHandler(successHandler.get()));
        }

        if (formLoginEnabled) {
            http.formLogin(form -> form
                    .loginPage(loginPage)
                    .loginProcessingUrl("/login")
                    .usernameParameter("username")
                    .passwordParameter("password")
                    .successHandler(new FormLoginSuccessHandler(cyphProperties))
                    .failureHandler(new FormLoginFailureHandler(cyphProperties)));
        }

        return http.build();
    }
}
