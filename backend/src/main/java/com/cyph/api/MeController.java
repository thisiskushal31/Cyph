package com.cyph.api;

import com.cyph.service.AllowedUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping(ApiV1.BASE)
public class MeController {

    private final AllowedUserService allowedUserService;

    public MeController(AllowedUserService allowedUserService) {
        this.allowedUserService = allowedUserService;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(
            Authentication authentication,
            @AuthenticationPrincipal OidcUser oidcUser,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        String email = emailFrom(oidcUser, oauth2User, authentication);
        if (email == null || email.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        boolean admin = allowedUserService.isAdmin(email);
        String name = nameFrom(oidcUser, oauth2User, email);
        return ResponseEntity.ok(Map.<String, Object>of(
                "email", email,
                "name", name != null ? name : email,
                "admin", admin
        ));
    }

    private static String emailFrom(OidcUser oidcUser, OAuth2User oauth2User, Authentication authentication) {
        if (oidcUser != null && oidcUser.getEmail() != null) return oidcUser.getEmail();
        if (oauth2User != null && oauth2User.getAttribute("email") != null) return (String) oauth2User.getAttribute("email");
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            return authentication.getName();
        }
        return null;
    }

    private static String nameFrom(OidcUser oidcUser, OAuth2User oauth2User, String email) {
        if (oidcUser != null && oidcUser.getFullName() != null && !oidcUser.getFullName().isBlank()) return oidcUser.getFullName();
        if (oauth2User != null) {
            String n = oauth2User.getAttribute("name");
            if (n != null && !n.isBlank()) return n;
        }
        return email != null ? email.split("@")[0].replace(".", " ") : null;
    }
}
