package com.cyph.api;

import com.cyph.api.dto.AddUserRequest;
import com.cyph.service.AllowedUserService;
import com.cyph.service.AuditService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AllowedUserService allowedUserService;
    private final AuditService auditService;

    public AdminController(AllowedUserService allowedUserService, AuditService auditService) {
        this.allowedUserService = allowedUserService;
        this.auditService = auditService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<AllowedUserService.AllowedUserDto>> listUsers(
            Authentication authentication,
            @AuthenticationPrincipal OidcUser oidcUser,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        String email = emailFrom(oidcUser, oauth2User, authentication);
        if (email == null || !allowedUserService.isAdmin(email)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(allowedUserService.listAll());
    }

    @PostMapping("/users")
    public ResponseEntity<?> addUser(@Valid @RequestBody AddUserRequest request,
                                    Authentication authentication,
                                    @AuthenticationPrincipal OidcUser oidcUser,
                                    @AuthenticationPrincipal OAuth2User oauth2User) {
        String adminEmail = emailFrom(oidcUser, oauth2User, authentication);
        if (adminEmail == null || !allowedUserService.isAdmin(adminEmail)) {
            return ResponseEntity.status(403).build();
        }
        return allowedUserService.addByAdmin(request.getEmail())
                .map(u -> ResponseEntity.ok(Map.of("email", u.getEmail(), "source", u.getSource().name())))
                .orElse(ResponseEntity.badRequest().body(Map.of("message", "User already exists")));
    }

    @DeleteMapping("/users/{email}")
    public ResponseEntity<?> removeUser(@PathVariable String email,
                                        Authentication authentication,
                                        @AuthenticationPrincipal OidcUser oidcUser,
                                        @AuthenticationPrincipal OAuth2User oauth2User) {
        String adminEmail = emailFrom(oidcUser, oauth2User, authentication);
        if (adminEmail == null || !allowedUserService.isAdmin(adminEmail)) {
            return ResponseEntity.status(403).build();
        }
        if (email.equalsIgnoreCase(adminEmail)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cannot remove yourself"));
        }
        boolean removed = allowedUserService.removeByAdmin(email);
        return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @PatchMapping("/users/{email}/admin")
    public ResponseEntity<?> setAdmin(@PathVariable String email,
                                      @RequestParam boolean admin,
                                      Authentication authentication,
                                      @AuthenticationPrincipal OidcUser oidcUser,
                                      @AuthenticationPrincipal OAuth2User oauth2User) {
        String currentEmail = emailFrom(oidcUser, oauth2User, authentication);
        if (currentEmail == null || !allowedUserService.isAdmin(currentEmail)) {
            return ResponseEntity.status(403).build();
        }
        allowedUserService.setAdmin(email, admin);
        return ResponseEntity.ok(Map.of("email", email, "admin", admin));
    }

    @GetMapping("/audit-log")
    public ResponseEntity<Page<AuditService.AuditLogDto>> getAuditLog(Pageable pageable,
                                                                      Authentication authentication,
                                                                      @AuthenticationPrincipal OidcUser oidcUser,
                                                                      @AuthenticationPrincipal OAuth2User oauth2User) {
        String email = emailFrom(oidcUser, oauth2User, authentication);
        if (email == null || !allowedUserService.isAdmin(email)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(auditService.getAuditLog(pageable));
    }

    private static String emailFrom(OidcUser oidcUser, OAuth2User oauth2User, Authentication authentication) {
        if (oidcUser != null && oidcUser.getEmail() != null) return oidcUser.getEmail();
        if (oauth2User != null && oauth2User.getAttribute("email") != null) return (String) oauth2User.getAttribute("email");
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            return authentication.getName();
        }
        return null;
    }
}
