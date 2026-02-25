package com.cyph.api;

import com.cyph.api.dto.AddUserRequest;
import com.cyph.service.AllowedUserService;
import com.cyph.service.AuditService;
import com.cyph.service.GroupSendPermissionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AllowedUserService allowedUserService;
    private final AuditService auditService;
    private final GroupSendPermissionService groupSendPermissionService;

    public AdminController(AllowedUserService allowedUserService, AuditService auditService,
                           GroupSendPermissionService groupSendPermissionService) {
        this.allowedUserService = allowedUserService;
        this.auditService = auditService;
        this.groupSendPermissionService = groupSendPermissionService;
    }

    @GetMapping("/users")
    public ResponseEntity<?> listUsers(
            Authentication authentication,
            @AuthenticationPrincipal OidcUser oidcUser,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        String email = emailFrom(oidcUser, oauth2User, authentication);
        if (email == null || !allowedUserService.isAdmin(email)) {
            return forbidden();
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
            return forbidden();
        }
        try {
            return allowedUserService.addByAdmin(
                            request.getEmail(),
                            request.getUsername(),
                            request.getPassword(),
                            request.getGroup(),
                            adminEmail)
                    .map(u -> ResponseEntity.ok(Map.of("email", u.getEmail(), "source", u.getSource().name())))
                    .orElse(ResponseEntity.badRequest().body(Map.of("message", "User already exists")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/users/{email}")
    public ResponseEntity<?> removeUser(@PathVariable String email,
                                        Authentication authentication,
                                        @AuthenticationPrincipal OidcUser oidcUser,
                                        @AuthenticationPrincipal OAuth2User oauth2User) {
        String adminEmail = emailFrom(oidcUser, oauth2User, authentication);
        if (adminEmail == null || !allowedUserService.isAdmin(adminEmail)) {
            return forbidden();
        }
        String decodedEmail = decodePathEmail(email);
        if (decodedEmail.equalsIgnoreCase(adminEmail)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cannot remove yourself"));
        }
        if (allowedUserService.isAdmin(decodedEmail)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cannot remove an admin user"));
        }
        boolean removed = allowedUserService.removeByAdmin(decodedEmail);
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
            return forbidden();
        }
        allowedUserService.setAdmin(decodePathEmail(email), admin);
        return ResponseEntity.ok(Map.of("email", decodePathEmail(email), "admin", admin));
    }

    @GetMapping("/groups")
    public ResponseEntity<?> listGroups(
            Authentication authentication,
            @AuthenticationPrincipal OidcUser oidcUser,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        String email = emailFrom(oidcUser, oauth2User, authentication);
        if (email == null || !allowedUserService.isAdmin(email)) {
            return forbidden();
        }
        return ResponseEntity.ok(groupSendPermissionService.listGroups());
    }

    @PostMapping("/groups")
    public ResponseEntity<?> createGroup(@RequestBody Map<String, String> body,
                                         Authentication authentication,
                                         @AuthenticationPrincipal OidcUser oidcUser,
                                         @AuthenticationPrincipal OAuth2User oauth2User) {
        String email = emailFrom(oidcUser, oauth2User, authentication);
        if (email == null || !allowedUserService.isAdmin(email)) {
            return forbidden();
        }
        String name = body != null ? body.get("name") : null;
        try {
            GroupSendPermissionService.GroupDto g = groupSendPermissionService.createGroup(name);
            return ResponseEntity.status(201).body(g);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/group-permissions")
    public ResponseEntity<?> listGroupPermissions(
            Authentication authentication,
            @AuthenticationPrincipal OidcUser oidcUser,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        String email = emailFrom(oidcUser, oauth2User, authentication);
        if (email == null || !allowedUserService.isAdmin(email)) {
            return forbidden();
        }
        return ResponseEntity.ok(groupSendPermissionService.listPermissions());
    }

    @PostMapping("/group-permissions")
    public ResponseEntity<?> addGroupPermission(@RequestBody Map<String, String> body,
                                                 Authentication authentication,
                                                 @AuthenticationPrincipal OidcUser oidcUser,
                                                 @AuthenticationPrincipal OAuth2User oauth2User) {
        String email = emailFrom(oidcUser, oauth2User, authentication);
        if (email == null || !allowedUserService.isAdmin(email)) {
            return forbidden();
        }
        String from = body != null ? body.get("fromGroupName") : null;
        String to = body != null ? body.get("toGroupName") : null;
        try {
            GroupSendPermissionService.GroupPermissionDto p = groupSendPermissionService.addPermission(from, to);
            return ResponseEntity.status(201).body(p);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/group-permissions")
    public ResponseEntity<?> removeGroupPermission(@RequestParam Long fromGroupId, @RequestParam Long toGroupId,
                                                    Authentication authentication,
                                                    @AuthenticationPrincipal OidcUser oidcUser,
                                                    @AuthenticationPrincipal OAuth2User oauth2User) {
        String email = emailFrom(oidcUser, oauth2User, authentication);
        if (email == null || !allowedUserService.isAdmin(email)) {
            return forbidden();
        }
        groupSendPermissionService.removePermission(fromGroupId, toGroupId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/audit-log")
    public ResponseEntity<?> getAuditLog(Pageable pageable,
                                                                      Authentication authentication,
                                                                      @AuthenticationPrincipal OidcUser oidcUser,
                                                                      @AuthenticationPrincipal OAuth2User oauth2User) {
        String email = emailFrom(oidcUser, oauth2User, authentication);
        if (email == null || !allowedUserService.isAdmin(email)) {
            return forbidden();
        }
        return ResponseEntity.ok(auditService.getAuditLog(pageable));
    }

    private static ResponseEntity<?> forbidden() {
        return ResponseEntity.status(403).body(Map.of("message", "Admin access required. Your account must be in cyph.auth.admin-emails or have Admin toggled in the users list."));
    }

    /** Decode path variable email in case it arrives encoded (e.g. kushal%40test). */
    private static String decodePathEmail(String email) {
        if (email == null || email.isBlank()) return email;
        try {
            return URLDecoder.decode(email, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return email;
        }
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
