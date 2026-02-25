package com.cyph.service;

import com.cyph.domain.AuditLog;
import com.cyph.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Audit logging with no PII. Only event type, timestamp, message id (UUID), and group names.
 */
@Service
public class AuditService {

    private final AuditLogRepository repository;

    public AuditService(AuditLogRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void logLogin(String actorIdentifier) {
        AuditLog e = new AuditLog();
        e.setEventType(AuditLog.EventType.LOGIN);
        e.setOccurredAt(Instant.now());
        e.setActorIdentifier(actorIdentifier != null && !actorIdentifier.isBlank() ? actorIdentifier : null);
        repository.save(e);
    }

    @Transactional
    public void logMessageSent(boolean sameGroup, List<String> senderGroupNames, List<String> recipientGroupNames, String messageId) {
        AuditLog e = new AuditLog();
        e.setEventType(sameGroup ? AuditLog.EventType.MESSAGE_SENT_SAME_GROUP : AuditLog.EventType.MESSAGE_SENT_CROSS_GROUP);
        e.setOccurredAt(Instant.now());
        e.setMessageId(messageId);
        e.setSenderGroupNames(senderGroupNames != null ? String.join(",", senderGroupNames) : null);
        e.setRecipientGroupNames(recipientGroupNames != null ? String.join(",", recipientGroupNames) : null);
        e.setSameGroup(sameGroup);
        repository.save(e);
    }

    @Transactional
    public void logMessageViewed(String messageId) {
        AuditLog e = new AuditLog();
        e.setEventType(AuditLog.EventType.MESSAGE_VIEWED);
        e.setOccurredAt(Instant.now());
        e.setMessageId(messageId);
        repository.save(e);
    }

    @Transactional
    public void logMessageDeleted(String messageId) {
        AuditLog e = new AuditLog();
        e.setEventType(AuditLog.EventType.MESSAGE_DELETED);
        e.setOccurredAt(Instant.now());
        e.setMessageId(messageId);
        repository.save(e);
    }

    public Page<AuditLogDto> getAuditLog(Pageable pageable) {
        return repository.findAllByOrderByOccurredAtDesc(pageable)
                .map(this::toDto);
    }

    private AuditLogDto toDto(AuditLog a) {
        return new AuditLogDto(
                a.getEventType().name(),
                a.getOccurredAt(),
                a.getMessageId(),
                a.getSenderGroupNames(),
                a.getRecipientGroupNames(),
                a.getSameGroup(),
                a.getActorIdentifier()
        );
    }

    public static record AuditLogDto(String eventType, Instant occurredAt, String messageId,
                                     String senderGroupNames, String recipientGroupNames, Boolean sameGroup,
                                     String actorIdentifier) {}
}
