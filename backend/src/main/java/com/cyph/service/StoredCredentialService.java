package com.cyph.service;

import com.cyph.domain.AllowedUser;
import com.cyph.domain.Group;
import com.cyph.domain.StoredCredential;
import com.cyph.domain.StoredCredentialGroupRecipient;
import com.cyph.domain.StoredCredentialUserRecipient;
import com.cyph.repository.StoredCredentialGroupRecipientRepository;
import com.cyph.repository.StoredCredentialRepository;
import com.cyph.repository.StoredCredentialUserRecipientRepository;
import com.cyph.repository.AllowedUserRepository;
import com.cyph.repository.GroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Manages shared (admin-pushed) and personal (user-owned) credentials.
 * Secrets are encrypted at rest using the existing EncryptionService.
 */
@Service
public class StoredCredentialService {

    private final StoredCredentialRepository credentialRepository;
    private final StoredCredentialUserRecipientRepository userRecipientRepository;
    private final StoredCredentialGroupRecipientRepository groupRecipientRepository;
    private final AllowedUserRepository userRepository;
    private final GroupRepository groupRepository;
    private final EncryptionService encryptionService;
    private final AuditService auditService;

    public StoredCredentialService(StoredCredentialRepository credentialRepository,
                                   StoredCredentialUserRecipientRepository userRecipientRepository,
                                   StoredCredentialGroupRecipientRepository groupRecipientRepository,
                                   AllowedUserRepository userRepository,
                                   GroupRepository groupRepository,
                                   EncryptionService encryptionService,
                                   AuditService auditService) {
        this.credentialRepository = credentialRepository;
        this.userRecipientRepository = userRecipientRepository;
        this.groupRecipientRepository = groupRecipientRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.encryptionService = encryptionService;
        this.auditService = auditService;
    }

    /** List entry for extension / API: no secret. */
    public record CredentialListItem(Long id, String label, String url, String usernameMeta, String source) {}

    /** Create a shared credential and assign to users and/or groups. */
    @Transactional
    public StoredCredential createShared(String adminEmail, String label, String url, String usernameMeta, String secretPlain,
                                        List<String> assignToUserEmails, List<String> assignToGroupNames) {
        StoredCredential c = new StoredCredential();
        c.setType(StoredCredential.Type.SHARED);
        c.setLabel(label != null ? label.trim() : "");
        c.setUrl(url != null ? url.trim() : null);
        c.setUsernameMeta(usernameMeta != null ? usernameMeta.trim() : null);
        c.setCreatedBy(adminEmail);
        encryptAndSetSecret(c, secretPlain);
        final StoredCredential saved = credentialRepository.save(c);

        if (assignToUserEmails != null) {
            for (String email : assignToUserEmails) {
                if (email == null || email.isBlank()) continue;
                userRepository.findByEmailIgnoreCase(email.trim()).ifPresent(user -> {
                    StoredCredentialUserRecipient r = new StoredCredentialUserRecipient();
                    r.setCredential(saved);
                    r.setUser(user);
                    userRecipientRepository.save(r);
                });
            }
        }
        if (assignToGroupNames != null) {
            for (String name : assignToGroupNames) {
                if (name == null || name.isBlank()) continue;
                groupRepository.findByNameIgnoreCase(name.trim()).ifPresent(group -> {
                    StoredCredentialGroupRecipient r = new StoredCredentialGroupRecipient();
                    r.setCredential(saved);
                    r.setGroup(group);
                    groupRecipientRepository.save(r);
                });
            }
        }

        String targetDetail = "users=" + (assignToUserEmails != null ? assignToUserEmails.size() : 0)
                + ",groups=" + (assignToGroupNames != null ? assignToGroupNames.size() : 0);
        auditService.logCredentialSharedPushed(adminEmail, saved.getLabel(), targetDetail);
        return saved;
    }

    /** List all shared credentials (admin). */
    public List<StoredCredential> listShared() {
        return credentialRepository.findByTypeOrderByCreatedAtDesc(StoredCredential.Type.SHARED);
    }

    /** DTO for admin to view/edit a shared credential (metadata + assignments; no secret). */
    public record SharedCredentialAdminDto(Long id, String label, String url, String usernameMeta,
                                          List<String> assignToUserEmails, List<String> assignToGroupNames) {}

    /** Get a single shared credential for admin edit (metadata and assignments only). */
    public Optional<SharedCredentialAdminDto> getSharedForAdmin(Long credentialId) {
        Optional<StoredCredential> opt = credentialRepository.findById(credentialId);
        if (opt.isEmpty() || opt.get().getType() != StoredCredential.Type.SHARED) return Optional.empty();
        StoredCredential c = opt.get();
        List<String> userEmails = userRecipientRepository.findByCredential_Id(credentialId).stream()
                .map(r -> r.getUser() != null ? r.getUser().getEmail() : null)
                .filter(Objects::nonNull)
                .toList();
        List<String> groupNames = groupRecipientRepository.findByCredential_Id(credentialId).stream()
                .map(r -> r.getGroup() != null ? r.getGroup().getName() : null)
                .filter(Objects::nonNull)
                .toList();
        return Optional.of(new SharedCredentialAdminDto(c.getId(), c.getLabel(), c.getUrl(), c.getUsernameMeta(), userEmails, groupNames));
    }

    /** Update a shared credential (admin). If secretPlain is null or empty, secret is left unchanged. */
    @Transactional
    public Optional<StoredCredential> updateShared(String adminEmail, Long credentialId,
                                                  String label, String url, String usernameMeta, String secretPlain,
                                                  List<String> assignToUserEmails, List<String> assignToGroupNames) {
        Optional<StoredCredential> opt = credentialRepository.findById(credentialId);
        if (opt.isEmpty() || opt.get().getType() != StoredCredential.Type.SHARED) return Optional.empty();
        StoredCredential c = opt.get();
        if (label != null && !label.isBlank()) c.setLabel(label.trim());
        if (url != null) c.setUrl(url.trim().isEmpty() ? null : url.trim());
        if (usernameMeta != null) c.setUsernameMeta(usernameMeta.trim().isEmpty() ? null : usernameMeta.trim());
        if (secretPlain != null && !secretPlain.isEmpty()) encryptAndSetSecret(c, secretPlain);
        credentialRepository.save(c);

        userRecipientRepository.deleteByCredential_Id(credentialId);
        groupRecipientRepository.deleteByCredential_Id(credentialId);
        credentialRepository.flush();
        final StoredCredential saved = c;
        if (assignToUserEmails != null) {
            for (String email : assignToUserEmails) {
                if (email == null || email.isBlank()) continue;
                userRepository.findByEmailIgnoreCase(email.trim()).ifPresent(user -> {
                    StoredCredentialUserRecipient r = new StoredCredentialUserRecipient();
                    r.setCredential(saved);
                    r.setUser(user);
                    userRecipientRepository.save(r);
                });
            }
        }
        if (assignToGroupNames != null) {
            for (String name : assignToGroupNames) {
                if (name == null || name.isBlank()) continue;
                groupRepository.findByNameIgnoreCase(name.trim()).ifPresent(group -> {
                    StoredCredentialGroupRecipient r = new StoredCredentialGroupRecipient();
                    r.setCredential(saved);
                    r.setGroup(group);
                    groupRecipientRepository.save(r);
                });
            }
        }

        auditService.logCredentialSharedUpdated(adminEmail, c.getLabel());
        return Optional.of(c);
    }

    /** Revoke (delete) a shared credential. */
    @Transactional
    public void deleteShared(String adminEmail, Long credentialId) {
        Optional<StoredCredential> opt = credentialRepository.findById(credentialId);
        if (opt.isEmpty() || opt.get().getType() != StoredCredential.Type.SHARED) return;
        String label = opt.get().getLabel();
        userRecipientRepository.deleteByCredential_Id(credentialId);
        groupRecipientRepository.deleteByCredential_Id(credentialId);
        credentialRepository.deleteById(credentialId);
        auditService.logCredentialSharedRevoked(adminEmail, label);
    }

    /** Create a personal credential. */
    @Transactional
    public StoredCredential createPersonal(String userEmail, String label, String url, String usernameMeta, String secretPlain) {
        AllowedUser user = userRepository.findByEmailIgnoreCase(userEmail != null ? userEmail.trim() : "").orElse(null);
        if (user == null) throw new IllegalArgumentException("User not found");
        StoredCredential c = new StoredCredential();
        c.setType(StoredCredential.Type.PERSONAL);
        c.setLabel(label != null ? label.trim() : "");
        c.setUrl(url != null ? url.trim() : null);
        c.setUsernameMeta(usernameMeta != null ? usernameMeta.trim() : null);
        c.setOwnerUserId(user.getId());
        encryptAndSetSecret(c, secretPlain);
        c = credentialRepository.save(c);
        auditService.logCredentialPersonalAdded(userEmail, c.getLabel());
        return c;
    }

    /** List personal credentials for a user. */
    public List<StoredCredential> listPersonal(String userEmail) {
        AllowedUser user = userRepository.findByEmailIgnoreCase(userEmail != null ? userEmail.trim() : "").orElse(null);
        if (user == null) return List.of();
        return credentialRepository.findByOwnerUserIdOrderByLabelAsc(user.getId());
    }

    /** Update a personal credential (must be owner). */
    @Transactional
    public Optional<StoredCredential> updatePersonal(String userEmail, Long credentialId, String label, String url, String usernameMeta, String secretPlain) {
        Optional<StoredCredential> opt = credentialRepository.findById(credentialId);
        if (opt.isEmpty() || opt.get().getType() != StoredCredential.Type.PERSONAL) return Optional.empty();
        AllowedUser user = userRepository.findByEmailIgnoreCase(userEmail != null ? userEmail.trim() : "").orElse(null);
        if (user == null || !user.getId().equals(opt.get().getOwnerUserId())) return Optional.empty();
        StoredCredential c = opt.get();
        if (label != null) c.setLabel(label.trim());
        if (url != null) c.setUrl(url.trim().isEmpty() ? null : url.trim());
        if (usernameMeta != null) c.setUsernameMeta(usernameMeta.trim().isEmpty() ? null : usernameMeta.trim());
        if (secretPlain != null && !secretPlain.isEmpty()) encryptAndSetSecret(c, secretPlain);
        credentialRepository.save(c);
        auditService.logCredentialPersonalUpdated(userEmail, c.getLabel());
        return Optional.of(c);
    }

    /** Delete a personal credential (must be owner). */
    @Transactional
    public boolean deletePersonal(String userEmail, Long credentialId) {
        Optional<StoredCredential> opt = credentialRepository.findById(credentialId);
        if (opt.isEmpty() || opt.get().getType() != StoredCredential.Type.PERSONAL) return false;
        AllowedUser user = userRepository.findByEmailIgnoreCase(userEmail != null ? userEmail.trim() : "").orElse(null);
        if (user == null || !user.getId().equals(opt.get().getOwnerUserId())) return false;
        String label = opt.get().getLabel();
        credentialRepository.deleteById(credentialId);
        auditService.logCredentialPersonalDeleted(userEmail, label);
        return true;
    }

    /** Unified list for extension: shared (assigned to user) + personal. */
    public List<CredentialListItem> listForExtension(String userEmail) {
        AllowedUser user = userRepository.findByEmailIgnoreCase(userEmail != null ? userEmail.trim() : "").orElse(null);
        if (user == null) return List.of();

        List<CredentialListItem> out = new ArrayList<>();
        Set<Long> seenIds = new HashSet<>();

        for (StoredCredentialUserRecipient r : userRecipientRepository.findByUser_Id(user.getId())) {
            StoredCredential c = r.getCredential();
            if (c != null && seenIds.add(c.getId())) {
                out.add(new CredentialListItem(c.getId(), c.getLabel(), c.getUrl(), c.getUsernameMeta(), "SHARED"));
            }
        }
        for (Group group : user.getGroups()) {
            for (StoredCredentialGroupRecipient r : groupRecipientRepository.findByGroup_Id(group.getId())) {
                StoredCredential c = r.getCredential();
                if (c != null && seenIds.add(c.getId())) {
                    out.add(new CredentialListItem(c.getId(), c.getLabel(), c.getUrl(), c.getUsernameMeta(), "SHARED"));
                }
            }
        }
        for (StoredCredential c : credentialRepository.findByOwnerUserIdOrderByLabelAsc(user.getId())) {
            if (seenIds.add(c.getId())) {
                out.add(new CredentialListItem(c.getId(), c.getLabel(), c.getUrl(), c.getUsernameMeta(), "PERSONAL"));
            }
        }
        out.sort(Comparator.comparing(CredentialListItem::label));
        return out;
    }

    /** Reveal secret for a credential the user is allowed to access. */
    @Transactional
    public Optional<String> reveal(String userEmail, Long credentialId) {
        AllowedUser user = userRepository.findByEmailIgnoreCase(userEmail != null ? userEmail.trim() : "").orElse(null);
        if (user == null) return Optional.empty();
        Optional<StoredCredential> opt = credentialRepository.findById(credentialId);
        if (opt.isEmpty()) return Optional.empty();
        StoredCredential c = opt.get();

        boolean allowed = false;
        String source = "";
        if (c.getType() == StoredCredential.Type.PERSONAL && user.getId().equals(c.getOwnerUserId())) {
            allowed = true;
            source = "PERSONAL";
        } else if (c.getType() == StoredCredential.Type.SHARED) {
            boolean byUser = userRecipientRepository.findByUser_Id(user.getId()).stream()
                    .anyMatch(r -> r.getCredential().getId().equals(credentialId));
            boolean byGroup = user.getGroups().stream().anyMatch(g ->
                    groupRecipientRepository.findByGroup_Id(g.getId()).stream()
                            .anyMatch(r -> r.getCredential().getId().equals(credentialId)));
            if (byUser || byGroup) {
                allowed = true;
                source = "SHARED";
            }
        }
        if (!allowed) return Optional.empty();

        String plain = decryptSecret(c);
        auditService.logCredentialRevealed(userEmail, credentialId, source);
        return Optional.of(plain);
    }

    /** Check if user owns this personal credential. */
    public boolean isOwnerOfPersonal(String userEmail, Long credentialId) {
        AllowedUser user = userRepository.findByEmailIgnoreCase(userEmail != null ? userEmail.trim() : "").orElse(null);
        if (user == null) return false;
        return credentialRepository.findById(credentialId)
                .filter(c -> c.getType() == StoredCredential.Type.PERSONAL && user.getId().equals(c.getOwnerUserId()))
                .isPresent();
    }

    private void encryptAndSetSecret(StoredCredential c, String secretPlain) {
        if (secretPlain == null) secretPlain = "";
        EncryptionService.EncryptionResult result = encryptionService.encrypt(secretPlain.getBytes(StandardCharsets.UTF_8));
        c.setEncryptionKey(result.key());
        c.setNonce(result.nonce());
        c.setEncryptedSecret(result.ciphertext());
    }

    private String decryptSecret(StoredCredential c) {
        byte[] plain = encryptionService.decrypt(c.getEncryptionKey(), c.getNonce(), c.getEncryptedSecret());
        return new String(plain, StandardCharsets.UTF_8);
    }
}
