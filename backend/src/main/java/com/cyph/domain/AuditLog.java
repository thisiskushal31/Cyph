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
        EXTENSION_LOGIN,
        MESSAGE_SENT_SAME_GROUP,
        MESSAGE_SENT_CROSS_GROUP,
        MESSAGE_VIEWED,
        MESSAGE_DELETED,
        USER_CREATED,
        USER_DELETED,
        USER_ADMIN_CHANGED,
        GROUP_CREATED,
        GROUP_PERMISSION_ADDED,
        GROUP_PERMISSION_REMOVED,
        CREDENTIAL_SHARED_PUSHED,
        CREDENTIAL_SHARED_UPDATED,
        CREDENTIAL_SHARED_REVOKED,
        CREDENTIAL_PERSONAL_ADDED,
        CREDENTIAL_PERSONAL_UPDATED,
        CREDENTIAL_PERSONAL_DELETED,
        CREDENTIAL_REVEALED
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

    /** Who performed the action (e.g. admin email for user/group events, principal for LOGIN). */
    @Column(name = "actor_identifier", length = 255)
    private String actorIdentifier;

    /** Target of the action (e.g. user email for user events, group name for group events). */
    @Column(name = "target_identifier", length = 255)
    private String targetIdentifier;

    /** Optional extra details (e.g. "admin=true", "fromGroup→toGroup"). */
    @Column(name = "details", length = 512)
    private String details;

    public AuditLog() {
    }

    public String getTargetIdentifier() {
        return targetIdentifier;
    }

    public void setTargetIdentifier(String targetIdentifier) {
        this.targetIdentifier = targetIdentifier;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
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
