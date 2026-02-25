package com.cyph.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Audit log for compliance. No PII: only event type, timestamp, message id (UUID),
 * and optional group names / same-group flag. No user emails or message content.
 */
@Entity
@Table(name = "audit_log", indexes = {
    @Index(name = "idx_audit_log_occurred_at", columnList = "occurred_at"),
    @Index(name = "idx_audit_log_event_type", columnList = "event_type")
})
public class AuditLog {

    public enum EventType {
        LOGIN,
        MESSAGE_SENT_SAME_GROUP,
        MESSAGE_SENT_CROSS_GROUP,
        MESSAGE_VIEWED,
        MESSAGE_DELETED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private EventType eventType;

    @NotNull
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    /** Message access token (UUID) for message-related events. Not PII. */
    @Column(name = "message_id", length = 64)
    private String messageId;

    /** Comma-separated group names (e.g. "tech,business"). Not PII. */
    @Column(name = "sender_group_names", length = 255)
    private String senderGroupNames;

    @Column(name = "recipient_group_names", length = 255)
    private String recipientGroupNames;

    @Column(name = "same_group")
    private Boolean sameGroup;

    /** For LOGIN events: the user who logged in (email or username). */
    @Column(name = "actor_identifier", length = 255)
    private String actorIdentifier;

    public AuditLog() {
    }

    public String getActorIdentifier() {
        return actorIdentifier;
    }

    public void setActorIdentifier(String actorIdentifier) {
        this.actorIdentifier = actorIdentifier;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public String getSenderGroupNames() {
        return senderGroupNames;
    }

    public void setSenderGroupNames(String senderGroupNames) {
        this.senderGroupNames = senderGroupNames;
    }

    public String getRecipientGroupNames() {
        return recipientGroupNames;
    }

    public void setRecipientGroupNames(String recipientGroupNames) {
        this.recipientGroupNames = recipientGroupNames;
    }

    public Boolean getSameGroup() {
        return sameGroup;
    }

    public void setSameGroup(Boolean sameGroup) {
        this.sameGroup = sameGroup;
    }
}
