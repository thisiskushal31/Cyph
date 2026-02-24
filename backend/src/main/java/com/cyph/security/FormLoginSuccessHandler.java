package com.cyph.security;

import com.cyph.config.CyphProperties;
import com.cyph.service.AllowedUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * After successful form (admin) login, return 200 JSON with redirectUrl so the SPA can navigate
 * without relying on 302 (which browsers/proxies may follow and hide from the client).
 * Also ensures the logged-in user exists in allowed_user so they appear in the recipients list.
 */
@Component
public class FormLoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(FormLoginSuccessHandler.class);

    private final CyphProperties cyphProperties;
    private final AllowedUserService allowedUserService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FormLoginSuccessHandler(CyphProperties cyphProperties, AllowedUserService allowedUserService) {
        this.cyphProperties = cyphProperties;
        this.allowedUserService = allowedUserService;
    }

    /** For use when SecurityConfig instantiates without the bean (allowedUserService will be null). */
    public FormLoginSuccessHandler(CyphProperties cyphProperties) {
        this.cyphProperties = cyphProperties;
        this.allowedUserService = null;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        log.info("Form login success: user={}", authentication != null ? authentication.getName() : "null");
        if (allowedUserService != null && authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            allowedUserService.ensureUserExists(authentication.getName());
        }

        // Force session creation so Set-Cookie (JSESSIONID) is added before we commit the response.
        request.getSession(true);
        String sessionId = request.getSession(false) != null ? request.getSession(false).getId() : null;
        log.debug("Form login success: sessionId={}, redirect param={}", sessionId, request.getParameter("redirect"));

        String redirect = request.getParameter("redirect");
        String path = (redirect != null && !redirect.isBlank())
                ? redirect.startsWith("/") ? redirect : "/" + redirect
                : "/send";
        // Strip query string and never redirect back to login after success
        int q = path.indexOf('?');
        if (q >= 0) path = path.substring(0, q);
        if ("/login".equals(path) || "login".equals(path) || path.startsWith("/login")) {
            path = "/send";
        }
        log.info("Form login success: returning redirectUrl={}", path);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), Map.of("redirectUrl", path));
    }
}
