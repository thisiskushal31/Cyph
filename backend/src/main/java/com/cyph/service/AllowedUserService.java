package com.cyph.service;

import com.cyph.config.CyphProperties;
import com.cyph.domain.AllowedUser;
import com.cyph.domain.Group;
import com.cyph.repository.AllowedUserRepository;
import com.cyph.repository.GroupRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Manages allowed users: admin-added and SSO. Used for access control and admin panel.
 */
@Service
public class AllowedUserService {

    private final AllowedUserRepository repository;
    private final GroupRepository groupRepository;
    private final CyphProperties cyphProperties;
    private final PasswordEncoder passwordEncoder;

    public AllowedUserService(AllowedUserRepository repository, GroupRepository groupRepository, CyphProperties cyphProperties, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.groupRepository = groupRepository;
        this.cyphProperties = cyphProperties;
        this.passwordEncoder = passwordEncoder;
    }

    /** Ensures an allowed_user row exists for this email (e.g. form-login user so they appear in recipients). */
    @Transactional
    public AllowedUser ensureUserExists(String email) {
        if (email == null || email.isBlank()) return null;
        return repository.findByEmailIgnoreCase(email)
                .orElseGet(() -> {
                    AllowedUser u = new AllowedUser();
                    u.setEmail(email.trim());
                    u.setSource(AllowedUser.Source.ADMIN_ADDED);
                    u.setAdmin(isSuperAdmin(email));
                    return repository.save(u);
                });
    }

    /** Check if this email is allowed to sign in (in DB or domain match when not requiring list). */
    public boolean isAllowedToSignIn(String email) {
        if (email == null || email.isBlank()) return false;
        if (repository.existsByEmailIgnoreCase(email)) return true;
        if (!cyphProperties.getAuth().isRequireAllowedUserList()) {
            List<String> domains = cyphProperties.getAuth().getAllowedDomains();
            if (domains.isEmpty()) return true;
            String domain = email.contains("@") ? email.substring(email.indexOf('@') + 1) : "";
            return domains.stream().anyMatch(d -> d.equalsIgnoreCase(domain));
        }
        return false;
    }

    /** Check if this email or form-login username is an admin (DB; super-admin from config is always admin). */
    public boolean isAdmin(String emailOrUsername) {
        if (emailOrUsername == null || emailOrUsername.isBlank()) return false;
        if (isSuperAdmin(emailOrUsername)) return true;
        return repository.findByEmailIgnoreCase(emailOrUsername).map(AllowedUser::isAdmin).orElse(false);
    }

    /**
     * True if this email is the single super-admin (from ADMIN_USERNAME / cyph.auth.form-login.username).
     * Super-admin cannot be deleted or demoted; other admins are managed via the admin panel.
     */
    public boolean isSuperAdmin(String email) {
        if (email == null || email.isBlank()) return false;
        if (!cyphProperties.getAuth().getFormLogin().isEnabled()) return false;
        String formUsername = cyphProperties.getAuth().getFormLogin().getUsername();
        String superAdminEmail = (formUsername != null && !formUsername.isBlank()) ? formUsername.trim() : "admin@localhost";
        return superAdminEmail.equalsIgnoreCase(email.trim());
    }

    @Transactional
    public AllowedUser upsertFromLogin(String email, String externalId, AllowedUser.Source source, List<String> groupNamesFromToken) {
        AllowedUser u = repository.findByEmailIgnoreCase(email)
                .map(user -> {
                    user.setExternalId(externalId);
                    user.setLastLoginAt(Instant.now());
                    return repository.save(user);
                })
                .orElseGet(() -> {
                    AllowedUser newUser = new AllowedUser();
                    newUser.setEmail(email);
                    newUser.setSource(source);
                    newUser.setExternalId(externalId);
                    newUser.setLastLoginAt(Instant.now());
                    newUser.setAdmin(isSuperAdmin(email));
                    return repository.save(newUser);
                });
        if (groupNamesFromToken != null) {
            syncGroupsForUser(u, groupNamesFromToken);
        }
        return u;
    }

    /** Replace user's groups with the given list (from SSO claim). Creates groups by name if they don't exist. */
    @Transactional
    public void syncGroupsForUser(AllowedUser user, List<String> groupNames) {
        if (user == null || groupNames == null) return;
        Set<Group> newGroups = new java.util.HashSet<>();
        for (String name : groupNames) {
            if (name == null || name.isBlank()) continue;
            String trimmed = name.trim();
            Group g = groupRepository.findByNameIgnoreCase(trimmed)
                    .orElseGet(() -> {
                        Group ng = new Group();
                        ng.setName(trimmed);
                        return groupRepository.save(ng);
                    });
            newGroups.add(g);
        }
        user.setGroups(newGroups);
        repository.save(user);
    }

    /** Returns group names for the user (empty if none). Used for same-group vs cross-group logic. */
    @Transactional(readOnly = true)
    public List<String> getGroupNamesForUser(String email) {
        return repository.findByEmailIgnoreCase(email)
                .map(u -> u.getGroups().stream().map(Group::getName).collect(Collectors.toList()))
                .orElse(List.of());
    }

    public List<AllowedUserDto> listAll() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(u -> new AllowedUserDto(u.getEmail(), u.getDisplayName(), u.getSource().name(), u.getExternalId(), u.isAdmin(), u.getCreatedAt(), u.getLastLoginAt(), u.getAddedBy()))
                .collect(Collectors.toList());
    }

    @Transactional
    public Optional<AllowedUser> addByAdmin(String email, String username, String password, String groupName, String addedBy) {
        if (repository.existsByEmailIgnoreCase(email)) return repository.findByEmailIgnoreCase(email);
        AllowedUser u = new AllowedUser();
        u.setEmail(email.trim());
        u.setSource(AllowedUser.Source.ADMIN_ADDED);
        u.setAdmin(false);
        if (username != null && !username.isBlank()) u.setDisplayName(username.trim());
        if (password != null && !password.isBlank()) u.setPasswordHash(passwordEncoder.encode(password));
        if (addedBy != null && !addedBy.isBlank()) u.setAddedBy(addedBy.trim());
        u = repository.save(u);
        if (groupName != null && !groupName.isBlank()) {
            String trimmed = groupName.trim();
            if (!groupRepository.existsByNameIgnoreCase(trimmed)) {
                throw new IllegalArgumentException("Group not found. Create the group first in Admin > Groups.");
            }
            syncGroupsForUser(u, List.of(trimmed));
        }
        return Optional.of(u);
    }

    @Transactional
    public boolean removeByAdmin(String email) {
        return repository.findByEmailIgnoreCase(email)
                .map(u -> {
                    repository.delete(u);
                    return true;
                })
                .orElse(false);
    }

    @Transactional
    public void setAdmin(String email, boolean admin) {
        repository.findByEmailIgnoreCase(email).ifPresent(u -> {
            u.setAdmin(admin);
            repository.save(u);
        });
    }

    public static record AllowedUserDto(String email, String displayName, String source, String externalId, boolean admin, Instant createdAt, Instant lastLoginAt, String addedBy) {}
}
