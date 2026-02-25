package com.cyph.service;

import com.cyph.config.CyphProperties;
import com.cyph.domain.SecretMessage;
import com.cyph.repository.SecretMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Application service for storing and retrieving secret messages.
 * Ensures recipient-only access and expiration; delegates crypto to EncryptionService.
 * Delete-after-read is best-effort: if delete fails, we log and still return the message;
 * the scheduled ExpiredMessageCleanupJob will remove expired rows (redundancy).
 */
@Service
public class SecretMessageService {

    private static final Logger log = LoggerFactory.getLogger(SecretMessageService.class);

    private final SecretMessageRepository repository;
    private final EncryptionService encryptionService;
    private final MailSenderService mailSenderService;
    private final AllowedUserService allowedUserService;
    private final AuditService auditService;
    private final CyphProperties cyphProperties;
    private final GroupSendPermissionService groupSendPermissionService;

    public SecretMessageService(SecretMessageRepository repository,
                                EncryptionService encryptionService,
                                MailSenderService mailSenderService,
                                AllowedUserService allowedUserService,
                                AuditService auditService,
                                CyphProperties cyphProperties,
                                GroupSendPermissionService groupSendPermissionService) {
        this.repository = repository;
        this.encryptionService = encryptionService;
        this.mailSenderService = mailSenderService;
        this.allowedUserService = allowedUserService;
        this.auditService = auditService;
        this.cyphProperties = cyphProperties;
        this.groupSendPermissionService = groupSendPermissionService;
    }

    public static record ViewResult(String message, boolean locked) {}

    /**
     * Encrypt the message, persist it, and send the recipient an email with the view link.
     * Same-group: message is readable by recipient. Cross-group: message is locked (stored but not viewable).
     */
    @Transactional
    public String store(String senderEmail, String recipientEmail, String plaintext, String senderDisplayName) {
        List<String> senderGroups = allowedUserService.getGroupNamesForUser(senderEmail);
        List<String> recipientGroups = allowedUserService.getGroupNamesForUser(recipientEmail);
        Set<String> senderSet = Set.copyOf(senderGroups);
        // Same group if they share at least one group, or both have no groups (e.g. admin-added users)
        boolean sameGroup = senderSet.isEmpty() && recipientGroups.isEmpty()
                || recipientGroups.stream().anyMatch(senderSet::contains);
        // Cross-group: locked unless there is an explicit send permission from sender's group to recipient's group
        boolean locked = !sameGroup && !groupSendPermissionService.canSendTo(senderGroups, recipientGroups);

        EncryptionService.EncryptionResult result = encryptionService.encrypt(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        int ttlHours = cyphProperties.getMessage().getDefaultTtlHours();
        Instant expiresAt = Instant.now().plusSeconds(ttlHours * 3600L);

        SecretMessage entity = new SecretMessage();
        entity.setSenderEmail(senderEmail);
        entity.setRecipientEmail(recipientEmail);
        entity.setKey(result.key());
        entity.setNonce(result.nonce());
        entity.setEncryptedData(result.ciphertext());
        entity.setExpiresAt(expiresAt);
        entity.setLocked(locked);
        repository.save(entity);

        auditService.logMessageSent(sameGroup, senderGroups, recipientGroups, entity.getAccessToken());

        String viewUrl = cyphProperties.getSiteUrl() + "/view/" + entity.getAccessToken();
        mailSenderService.sendSecretMessageNotification(recipientEmail, senderDisplayName, viewUrl, ttlHours);

        return entity.getAccessToken();
    }

    /**
     * Retrieve and decrypt the message only if the requester is the recipient and the message has not expired.
     * Cross-group (locked) messages: return ViewResult with locked=true and no content.
     */
    @Transactional
    public Optional<ViewResult> getPlaintext(String accessToken, String requesterEmail) {
        return repository.findByAccessToken(accessToken)
                .filter(secret -> secret.getRecipientEmail().equalsIgnoreCase(requesterEmail))
                .map(secret -> {
                    if (secret.getExpiresAt().isBefore(Instant.now())) {
                        auditService.logMessageDeleted(secret.getAccessToken());
                        deleteBestEffort(secret, "expired");
                        return null;
                    }
                    if (secret.isLocked()) {
                        return new ViewResult(null, true);
                    }
                    auditService.logMessageViewed(secret.getAccessToken());
                    byte[] plain = encryptionService.decrypt(secret.getKey(), secret.getNonce(), secret.getEncryptedData());
                    auditService.logMessageDeleted(secret.getAccessToken());
                    deleteBestEffort(secret, "after read");
                    return new ViewResult(new String(plain, java.nio.charset.StandardCharsets.UTF_8), false);
                })
                .filter(r -> r != null);
    }

    /** Delete and swallow failure so recipient still gets content; scheduled job provides redundancy. */
    private void deleteBestEffort(SecretMessage secret, String reason) {
        try {
            repository.delete(secret);
        } catch (Exception e) {
            log.warn("Best-effort delete failed ({}); scheduled cleanup will remove. id={}", reason, secret.getId(), e);
        }
    }

    /**
     * Delete all messages that have passed their expiration time. Logs each deletion for audit (no PII).
     */
    @Transactional
    public int deleteExpired() {
        Instant now = Instant.now();
        List<SecretMessage> expired = repository.findAllByExpiresAtBefore(now);
        for (SecretMessage secret : expired) {
            auditService.logMessageDeleted(secret.getAccessToken());
        }
        int deleted = repository.deleteExpiredBefore(now);
        return deleted;
    }
}
