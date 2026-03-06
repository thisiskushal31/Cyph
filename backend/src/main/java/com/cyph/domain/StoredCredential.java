package com.cyph.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * A stored credential (shared or personal). Secret is encrypted at rest.
 * Shared: created by admin, assigned via StoredCredentialUserRecipient / StoredCredentialGroupRecipient.
 * Personal: owned by ownerUserId, no recipients.
 */
@Entity
@Table(name = "stored_credential", indexes = {
    @Index(name = "idx_stored_credential_owner", columnList = "owner_user_id"),
    @Index(name = "idx_stored_credential_type", columnList = "type")
})
public class StoredCredential {

    public enum Type { SHARED, PERSONAL }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Type type = Type.PERSONAL;

    @NotBlank
    @Column(nullable = false, length = 256)
    private String label;

    /** Optional URL (e.g. https://monitor.example.com). */
    @Column(length = 2048)
    private String url;

    /** Optional username / metadata (not the Cyph user; the login name for the credential). */
    @Column(name = "username_meta", length = 512)
    private String usernameMeta;

    @JsonIgnore
    @NotNull
    @Column(name = "encryption_key", nullable = false)
    private byte[] encryptionKey;

    @JsonIgnore
    @NotNull
    @Column(nullable = false)
    private byte[] nonce;

    @JsonIgnore
    @NotNull
    @Column(name = "encrypted_secret", nullable = false)
    private byte[] encryptedSecret;

    /** For PERSONAL: the user who owns this. For SHARED: null. */
    @Column(name = "owner_user_id")
    private Long ownerUserId;

    /** For SHARED: admin who created. For audit. */
    @Column(name = "created_by", length = 256)
    private String createdBy;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public StoredCredential() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getUsernameMeta() { return usernameMeta; }
    public void setUsernameMeta(String usernameMeta) { this.usernameMeta = usernameMeta; }
    public byte[] getEncryptionKey() { return encryptionKey; }
    public void setEncryptionKey(byte[] encryptionKey) { this.encryptionKey = encryptionKey; }
    public byte[] getNonce() { return nonce; }
    public void setNonce(byte[] nonce) { this.nonce = nonce; }
    public byte[] getEncryptedSecret() { return encryptedSecret; }
    public void setEncryptedSecret(byte[] encryptedSecret) { this.encryptedSecret = encryptedSecret; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
