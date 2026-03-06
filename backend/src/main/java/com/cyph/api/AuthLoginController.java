package com.cyph.api;

import com.cyph.service.AllowedUserService;
import com.cyph.service.AuditService;
import com.cyph.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * POST /api/v1/auth/login: JSON-based form login so the SPA can log in without relying on
 * POST /login being proxied (which can return HTML in some dev setups).
 * POST /api/v1/auth/extension-login: same credentials, returns JWT for extension (no session).
 * GET /api/v1/auth/session-info: returns who the backend sees and whether they are admin (for debugging session/cookie issues).
 */
@RestController
@RequestMapping(ApiV1.BASE + "/auth")
public class AuthLoginController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final AllowedUserService allowedUserService;
    private final AuditService auditService;
    private final JwtService jwtService;

    public AuthLoginController(AuthenticationManager authenticationManager,
                               SecurityContextRepository securityContextRepository,
                               AllowedUserService allowedUserService,
                               AuditService auditService,
                               JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.allowedUserService = allowedUserService;
        this.auditService = auditService;
        this.jwtService = jwtService;
    }

    /** Returns current principal and isAdmin so you can verify what the backend sees (session/cookie debugging). */
    @GetMapping(value = "/session-info", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> sessionInfo(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null || authentication.getName().isBlank()) {
            return ResponseEntity.status(401).body(Map.of("authenticated", false, "message", "No session or not logged in."));
        }
        String principal = authentication.getName();
        boolean isAdmin = allowedUserService != null && allowedUserService.isAdmin(principal);
        return ResponseEntity.ok(Map.of(
                "authenticated", true,
                "principal", principal,
                "isAdmin", isAdmin
        ));
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest body, HttpServletRequest request, HttpServletResponse response) {
        String username = body.getUsername() != null ? body.getUsername().trim() : "";
        String password = body.getPassword() != null ? body.getPassword() : "";
        String redirectUrl = body.getRedirectUrl() != null && !body.getRedirectUrl().isBlank()
                ? body.getRedirectUrl().trim() : "/send";
        if (!redirectUrl.startsWith("/")) redirectUrl = "/" + redirectUrl;
        int q = redirectUrl.indexOf('?');
        if (q >= 0) redirectUrl = redirectUrl.substring(0, q);
        if ("/login".equals(redirectUrl) || redirectUrl.startsWith("/login")) redirectUrl = "/send";

        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
            SecurityContextHolder.getContext().setAuthentication(auth);
            request.getSession(true);

            if (allowedUserService != null && auth.getName() != null && !auth.getName().isBlank()) {
                allowedUserService.ensureUserExists(auth.getName());
            }
            if (auditService != null) {
                auditService.logLogin(auth.getName());
            }

            securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);

            return ResponseEntity.ok(Map.of("redirectUrl", redirectUrl));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid username or password."));
        }
    }

    /** Extension login: same username/password; returns JWT only. Used by Chrome extension. */
    @PostMapping(value = "/extension-login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> extensionLogin(@Valid @RequestBody LoginRequest body) {
        String username = body.getUsername() != null ? body.getUsername().trim() : "";
        String password = body.getPassword() != null ? body.getPassword() : "";
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
            if (allowedUserService != null && auth.getName() != null && !auth.getName().isBlank()) {
                allowedUserService.ensureUserExists(auth.getName());
            }
            if (auditService != null) {
                auditService.logExtensionLogin(auth.getName());
            }
            String token = jwtService.issueToken(auth.getName());
            return ResponseEntity.ok(Map.of("accessToken", token, "token", token, "principal", auth.getName()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body(Map.of("message", "Invalid username or password."));
        }
    }

    public static class LoginRequest {
        @NotBlank(message = "username required")
        private String username;
        private String password;
        private String redirectUrl;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getRedirectUrl() { return redirectUrl; }
        public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }
    }
}
