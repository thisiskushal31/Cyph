package com.cyph.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Persisted secret message: encrypted payload + metadata.
 * Key and nonce are stored so only the backend can decrypt for the verified recipient.
 */
@Entity
@Table(name = "secret_message", indexes = {
    @Index(name = "idx_secret_message_access_token", columnList = "access_token", unique = true),
    @Index(name = "idx_secret_message_expires_at", columnList = "expires_at")
})
public class SecretMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Email
    @NotBlank
    @Column(name = "sender_email", nullable = false)
    private String senderEmail;

    @Email
    @NotBlank
    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @NotNull
    @Column(name = "encryption_key", nullable = false)
    private byte[] key;

    @NotNull
    @Column(nullable = false)
    private byte[] nonce;

    @NotNull
    @Column(name = "encrypted_data", nullable = false)
    private byte[] encryptedData;

    @NotBlank
    @Column(name = "access_token", nullable = false, unique = true, length = 64)
    private String accessToken;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** When true, message was sent cross-group; recipient cannot view content. */
    @Column(name = "locked", nullable = false)
    private boolean locked = false;

    @PrePersist
    public void prePersist() {
        if (accessToken == null || accessToken.isBlank()) {
            accessToken = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    // --- Getters / Setters (and no-arg constructor for JPA) ---

    public SecretMessage() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public byte[] getKey() {
        return key;
    }

    public void setKey(byte[] key) {
        this.key = key;
    }

    public byte[] getNonce() {
        return nonce;
    }

    public void setNonce(byte[] nonce) {
        this.nonce = nonce;
    }

    public byte[] getEncryptedData() {
        return encryptedData;
    }

    public void setEncryptedData(byte[] encryptedData) {
        this.encryptedData = encryptedData;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }
}
