package com.cyph.api;

import com.cyph.api.dto.SendSecretRequest;
import com.cyph.service.SecretMessageService.ViewResult;
import com.cyph.service.SecretMessageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for sending and viewing secret messages.
 * Authenticated user email is taken from OIDC or OAuth2 token.
 */
@RestController
@RequestMapping("/api")
public class SecretMessageController {

    private final SecretMessageService secretMessageService;

    public SecretMessageController(SecretMessageService secretMessageService) {
        this.secretMessageService = secretMessageService;
    }

    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> send(@Valid @RequestBody SendSecretRequest request,
                                                   Authentication authentication,
                                                   @AuthenticationPrincipal OidcUser oidcUser,
                                                   @AuthenticationPrincipal OAuth2User oauth2User) {
        String senderEmail = emailFromPrincipal(oidcUser, oauth2User, authentication);
        if (senderEmail == null || senderEmail.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        String senderName = senderDisplayName(oidcUser, oauth2User, senderEmail);
        String token = secretMessageService.store(
                senderEmail,
                request.getRecipientEmail(),
                request.getMessage(),
                senderName
        );
        return ResponseEntity.ok(Map.of("accessToken", token));
    }

    @GetMapping("/view/{accessToken}")
    public ResponseEntity<?> view(@PathVariable String accessToken,
                                  Authentication authentication,
                                  @AuthenticationPrincipal OidcUser oidcUser,
                                  @AuthenticationPrincipal OAuth2User oauth2User) {
        String email = emailFromPrincipal(oidcUser, oauth2User, authentication);
        if (email == null || email.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        var optional = secretMessageService.getPlaintext(accessToken, email);
        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var result = optional.get();
        if (result.locked()) {
            return ResponseEntity.status(403).body(Map.of("locked", true));
        }
        return ResponseEntity.ok(Map.of("message", result.message()));
    }

    private static String emailFromPrincipal(OidcUser oidcUser, OAuth2User oauth2User, Authentication authentication) {
        if (oidcUser != null && oidcUser.getEmail() != null) {
            return oidcUser.getEmail();
        }
        if (oauth2User != null && oauth2User.getAttribute("email") != null) {
            return oauth2User.getAttribute("email");
        }
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            return authentication.getName();
        }
        return null;
    }

    private static String senderDisplayName(OidcUser oidcUser, OAuth2User oauth2User, String email) {
        if (oidcUser != null && oidcUser.getFullName() != null && !oidcUser.getFullName().isBlank()) {
            return oidcUser.getFullName();
        }
        if (oauth2User != null) {
            String name = oauth2User.getAttribute("name");
            if (name != null && !name.isBlank()) return name;
        }
        return email != null ? email.split("@")[0].replace(".", " ") : "Someone";
    }
}
