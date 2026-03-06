package com.cyph.api;

import com.cyph.service.StoredCredentialService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Extension-only APIs (JWT auth). Base URL: same as Cyph deployment (e.g. https://cyph.company.com).
 * Extension uses: POST /api/v1/auth/extension-login then GET/POST/PUT/DELETE under /api/v1/extension/credentials.
 */
@RestController
@RequestMapping(ApiV1.BASE + "/extension/credentials")
public class ExtensionCredentialsController {

    private final StoredCredentialService credentialService;

    public ExtensionCredentialsController(StoredCredentialService credentialService) {
        this.credentialService = credentialService;
    }

    private static String principal(Authentication auth) {
        return auth != null && auth.isAuthenticated() && auth.getName() != null ? auth.getName() : null;
    }

    /** GET /api/v1/extension/credentials — unified list (shared + personal) for extension. */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StoredCredentialService.CredentialListItem>> list(Authentication authentication) {
        String email = principal(authentication);
        if (email == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(credentialService.listForExtension(email));
    }

    /** POST /api/v1/extension/credentials/{id}/reveal — reveal secret for one credential (shared or personal). */
    @PostMapping(value = "/{id}/reveal", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> reveal(@PathVariable Long id, Authentication authentication) {
        String email = principal(authentication);
        if (email == null) return ResponseEntity.status(401).build();
        return credentialService.reveal(email, id)
                .map(secret -> ResponseEntity.ok(Map.<String, Object>of("secret", secret)))
                .orElse(ResponseEntity.status(403).body(Map.of("message", "Not authorized or not found")));
    }

    /** POST /api/v1/extension/credentials — create personal credential (extension). */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> createPersonal(@Valid @RequestBody CreatePersonalRequest body, Authentication authentication) {
        String email = principal(authentication);
        if (email == null) return ResponseEntity.status(401).build();
        if (body == null || body.getLabel() == null || body.getLabel().isBlank())
            return ResponseEntity.badRequest().body(Map.of("message", "label required"));
        try {
            var c = credentialService.createPersonal(email, body.getLabel(), body.getUrl(), body.getUsernameMeta(), body.getSecret());
            return ResponseEntity.ok(Map.of("id", c.getId(), "label", c.getLabel()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    /** PUT /api/v1/extension/credentials/{id} — update personal credential. */
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updatePersonal(@PathVariable Long id, @RequestBody UpdatePersonalRequest body, Authentication authentication) {
        String email = principal(authentication);
        if (email == null) return ResponseEntity.status(401).build();
        var opt = credentialService.updatePersonal(email, id,
                body != null ? body.getLabel() : null,
                body != null ? body.getUrl() : null,
                body != null ? body.getUsernameMeta() : null,
                body != null ? body.getSecret() : null);
        return opt.isPresent() ? ResponseEntity.ok(Map.of("id", id)) : ResponseEntity.notFound().build();
    }

    /** DELETE /api/v1/extension/credentials/{id} — delete personal credential. */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePersonal(@PathVariable Long id, Authentication authentication) {
        String email = principal(authentication);
        if (email == null) return ResponseEntity.status(401).build();
        return credentialService.deletePersonal(email, id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    public static class CreatePersonalRequest {
        @NotBlank
        private String label;
        private String url;
        private String usernameMeta;
        private String secret;

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getUsernameMeta() { return usernameMeta; }
        public void setUsernameMeta(String usernameMeta) { this.usernameMeta = usernameMeta; }
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
    }

    public static class UpdatePersonalRequest {
        private String label;
        private String url;
        private String usernameMeta;
        private String secret;

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getUsernameMeta() { return usernameMeta; }
        public void setUsernameMeta(String usernameMeta) { this.usernameMeta = usernameMeta; }
        public String getSecret() { return secret; }
        public void setSecret(String secret) { this.secret = secret; }
    }
}
