package com.cyph.api;

import com.cyph.api.dto.AddUserRequest;
import com.cyph.api.dto.RemoveGroupPermissionRequest;
import com.cyph.api.dto.RemoveUserRequest;
import com.cyph.api.dto.SetAdminRequest;
import com.cyph.service.AllowedUserService;
import com.cyph.service.AuditService;
import com.cyph.service.GroupSendPermissionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin API (users, groups, group-permissions, audit-log).
 * All mutation endpoints use POST with a JSON body so credentials are sent reliably.
 */
@RestController
@RequestMapping(ApiV1.BASE + "/admin")
public class AdminController {

    private static final String MSG_UNAUTHENTICATED = "Not authenticated. Session may have expired; try logging in again.";
    private static final String MSG_FORBIDDEN = "Admin access required. Your account must have Admin toggled in the users list.";

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
        AuthResult auth = requireAdmin(oidcUser, oauth2User, authentication);
        if (auth.error() != null) return auth.error();
        return ResponseEntity.ok(allowedUserService.listAll());
    }

    @PostMapping("/users")
    public ResponseEntity<?> addUser(@Valid @RequestBody AddUserRequest request,
                                      Authentication authentication,
                                      @AuthenticationPrincipal OidcUser oidcUser,
                                      @AuthenticationPrincipal OAuth2User oauth2User) {
        AuthResult auth = requireAdmin(oidcUser, oauth2User, authentication);
        if (auth.error() != null) return auth.error();
        String adminEmail = auth.adminEmail();
        try {
            var added = allowedUserService.addByAdmin(
                    request.getEmail(),
                    request.getUsername(),
                    request.getPassword(),
                    request.getGroup(),
                    adminEmail);
            if (added.isEmpty()) return badRequest("User already exists");
            var u = added.get();
            auditService.logUserCreated(adminEmail, u.getEmail());
            return ResponseEntity.ok(Map.of("email", u.getEmail(), "source", u.getSource().name()));
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @PostMapping("/users/remove")
    public ResponseEntity<?> removeUser(@Valid @RequestBody RemoveUserRequest request,
                                        Authentication authentication,
                                        @AuthenticationPrincipal OidcUser oidcUser,
                                        @AuthenticationPrincipal OAuth2User oauth2User) {
        AuthResult auth = requireAdmin(oidcUser, oauth2User, authentication);
        if (auth.error() != null) return auth.error();
        String adminEmail = auth.adminEmail();

        String emailToRemove = request != null && request.getEmail() != null ? request.getEmail().trim() : "";
        if (emailToRemove.isBlank()) return badRequest("Email is required");
        if (emailToRemove.equalsIgnoreCase(adminEmail)) return badRequest("Cannot remove yourself");
        if (allowedUserService.isSuperAdmin(emailToRemove)) return badRequest("Cannot remove the super-admin user");
        if (allowedUserService.isAdmin(emailToRemove)) return badRequest("Cannot remove an admin user");

        boolean removed = allowedUserService.removeByAdmin(emailToRemove);
        if (removed) {
            auditService.logUserDeleted(adminEmail, emailToRemove);
            return ResponseEntity.noContent().build();
        }
        return notFound("User not found");
    }

    @PostMapping("/users/set-admin")
    public ResponseEntity<?> setAdmin(@Valid @RequestBody SetAdminRequest request,
                                     Authentication authentication,
                                     @AuthenticationPrincipal OidcUser oidcUser,
                                     @AuthenticationPrincipal OAuth2User oauth2User) {
        AuthResult auth = requireAdmin(oidcUser, oauth2User, authentication);
        if (auth.error() != null) return auth.error();
        String adminEmail = auth.adminEmail();

        String targetEmail = request.getEmail() != null ? request.getEmail().trim() : "";
        Boolean admin = request.getAdmin();
        if (targetEmail.isBlank()) return badRequest("Email is required");
        if (admin == null) return badRequest("Admin flag is required");
        if (!admin && allowedUserService.isSuperAdmin(targetEmail)) {
            return badRequest("Cannot demote the super-admin user");
        }

        allowedUserService.setAdmin(targetEmail, admin);
        auditService.logUserAdminChanged(adminEmail, targetEmail, admin);
        return ResponseEntity.ok(Map.of("email", targetEmail, "admin", admin));
    }

    @GetMapping("/groups")
    public ResponseEntity<?> listGroups(
            Authentication authentication,
            @AuthenticationPrincipal OidcUser oidcUser,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        AuthResult auth = requireAdmin(oidcUser, oauth2User, authentication);
        if (auth.error() != null) return auth.error();
        return ResponseEntity.ok(groupSendPermissionService.listGroups());
    }

    @PostMapping("/groups")
    public ResponseEntity<?> createGroup(@RequestBody Map<String, String> body,
                                         Authentication authentication,
                                         @AuthenticationPrincipal OidcUser oidcUser,
                                         @AuthenticationPrincipal OAuth2User oauth2User) {
        AuthResult auth = requireAdmin(oidcUser, oauth2User, authentication);
        if (auth.error() != null) return auth.error();
        String adminEmail = auth.adminEmail();
        String name = body != null ? body.get("name") : null;
        try {
            GroupSendPermissionService.GroupDto g = groupSendPermissionService.createGroup(name);
            auditService.logGroupCreated(adminEmail, g.name());
            return ResponseEntity.status(201).body(g);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @GetMapping("/group-permissions")
    public ResponseEntity<?> listGroupPermissions(
            Authentication authentication,
            @AuthenticationPrincipal OidcUser oidcUser,
            @AuthenticationPrincipal OAuth2User oauth2User) {
        AuthResult auth = requireAdmin(oidcUser, oauth2User, authentication);
        if (auth.error() != null) return auth.error();
        return ResponseEntity.ok(groupSendPermissionService.listPermissions());
    }

    @PostMapping("/group-permissions")
    public ResponseEntity<?> addGroupPermission(@RequestBody Map<String, String> body,
                                                Authentication authentication,
                                                @AuthenticationPrincipal OidcUser oidcUser,
                                                @AuthenticationPrincipal OAuth2User oauth2User) {
        AuthResult auth = requireAdmin(oidcUser, oauth2User, authentication);
        if (auth.error() != null) return auth.error();
        String adminEmail = auth.adminEmail();
        String from = body != null ? body.get("fromGroupName") : null;
        String to = body != null ? body.get("toGroupName") : null;
        try {
            GroupSendPermissionService.GroupPermissionDto p = groupSendPermissionService.addPermission(from, to);
            auditService.logGroupPermissionAdded(adminEmail, p.fromGroupName(), p.toGroupName());
            return ResponseEntity.status(201).body(p);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @PostMapping("/group-permissions/remove")
    public ResponseEntity<?> removeGroupPermission(@Valid @RequestBody RemoveGroupPermissionRequest request,
                                                    Authentication authentication,
                                                    @AuthenticationPrincipal OidcUser oidcUser,
                                                    @AuthenticationPrincipal OAuth2User oauth2User) {
        AuthResult auth = requireAdmin(oidcUser, oauth2User, authentication);
        if (auth.error() != null) return auth.error();
        if (request.getFromGroupId() == null || request.getToGroupId() == null) {
            return badRequest("fromGroupId and toGroupId are required");
        }
        Long fromId = request.getFromGroupId();
        Long toId = request.getToGroupId();
        groupSendPermissionService.removePermission(fromId, toId);
        auditService.logGroupPermissionRemovedByIds(auth.adminEmail(), fromId, toId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/audit-log")
    public ResponseEntity<?> getAuditLog(Pageable pageable,
                                         Authentication authentication,
                                         @AuthenticationPrincipal OidcUser oidcUser,
                                         @AuthenticationPrincipal OAuth2User oauth2User) {
        AuthResult auth = requireAdmin(oidcUser, oauth2User, authentication);
        if (auth.error() != null) return auth.error();
        return ResponseEntity.ok(auditService.getAuditLog(pageable));
    }

    // --- Auth & error helpers (consistent responses for all admin endpoints) ---

    /** Returns current user email from OIDC, OAuth2, or form auth; null if not authenticated. */
    private String getCurrentEmail(OidcUser oidcUser, OAuth2User oauth2User, Authentication authentication) {
        if (oidcUser != null && oidcUser.getEmail() != null) return oidcUser.getEmail();
        if (oauth2User != null && oauth2User.getAttribute("email") != null) {
            return (String) oauth2User.getAttribute("email");
        }
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            return authentication.getName();
        }
        return null;
    }

    /**
     * Returns admin email if current user is authenticated and is an admin;
     * otherwise returns an error response (left) and null email (right).
     */
    private AuthResult requireAdmin(OidcUser oidcUser, OAuth2User oauth2User, Authentication authentication) {
        String email = getCurrentEmail(oidcUser, oauth2User, authentication);
        if (email == null || email.isBlank()) {
            return new AuthResult(ResponseEntity.status(401).body(Map.of("message", MSG_UNAUTHENTICATED)), null);
        }
        if (!allowedUserService.isAdmin(email)) {
            return new AuthResult(ResponseEntity.status(403).body(Map.of("message", MSG_FORBIDDEN)), null);
        }
        return new AuthResult(null, email);
    }

    private record AuthResult(ResponseEntity<?> error, String adminEmail) {}

    private static ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message != null ? message : "Bad request"));
    }

    private static ResponseEntity<?> notFound(String message) {
        return ResponseEntity.status(404).body(Map.of("message", message != null ? message : "Not found"));
    }
}
