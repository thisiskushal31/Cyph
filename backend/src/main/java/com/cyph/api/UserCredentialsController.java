package com.cyph.api;

import com.cyph.domain.StoredCredential;
import com.cyph.service.StoredCredentialService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Session-authenticated APIs for "My credentials" in the web app.
 * GET/POST/PUT/DELETE /api/v1/credentials — list and manage current user's personal credentials.
 */
@RestController
@RequestMapping(ApiV1.BASE + "/credentials")
public class UserCredentialsController {

    private final StoredCredentialService credentialService;

    public UserCredentialsController(StoredCredentialService credentialService) {
        this.credentialService = credentialService;
    }

    private static String principal(Authentication auth) {
        return auth != null && auth.isAuthenticated() && auth.getName() != null ? auth.getName() : null;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<StoredCredential>> list(Authentication authentication) {
        String email = principal(authentication);
        if (email == null) return ResponseEntity.status(401).build();
        return ResponseEntity.ok(credentialService.listPersonal(email));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> create(@Valid @RequestBody CreateRequest body, Authentication authentication) {
        String email = principal(authentication);
        if (email == null) return ResponseEntity.status(401).build();
        if (body == null || body.getLabel() == null || body.getLabel().isBlank())
            return ResponseEntity.badRequest().body(Map.of("message", "label required"));
        try {
            StoredCredential c = credentialService.createPersonal(
                    email, body.getLabel(), body.getUrl(), body.getUsernameMeta(), body.getSecret());
            return ResponseEntity.status(201).body(Map.of("id", c.getId(), "label", c.getLabel()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody UpdateRequest body, Authentication authentication) {
        String email = principal(authentication);
        if (email == null) return ResponseEntity.status(401).build();
        var opt = credentialService.updatePersonal(email, id,
                body != null ? body.getLabel() : null,
                body != null ? body.getUrl() : null,
                body != null ? body.getUsernameMeta() : null,
                body != null ? body.getSecret() : null);
        return opt.isPresent() ? ResponseEntity.ok(Map.of("id", id)) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication authentication) {
        String email = principal(authentication);
        if (email == null) return ResponseEntity.status(401).build();
        return credentialService.deletePersonal(email, id) ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    public static class CreateRequest {
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

    public static class UpdateRequest {
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
