package com.cyph.security;

import com.cyph.config.CyphProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.io.IOException;

/**
 * Logs form login failures and redirects to the frontend login page with ?error.
 */
public class FormLoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(FormLoginFailureHandler.class);

    private final CyphProperties cyphProperties;

    public FormLoginFailureHandler(CyphProperties cyphProperties) {
        this.cyphProperties = cyphProperties;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                         AuthenticationException exception) throws IOException {
        String username = request.getParameter("username");
        log.warn("Form login failed: username=[{}], reason=[{}]", username, exception.getMessage());
        String target = cyphProperties.getSiteUrl() + "/login?error";
        response.sendRedirect(target);
    }
}
