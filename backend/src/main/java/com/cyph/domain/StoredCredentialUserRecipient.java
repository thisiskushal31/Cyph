package com.cyph.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

/**
 * Assignment of a SHARED credential to a user (by email / user id).
 */
@Entity
@Table(name = "stored_credential_user_recipient", indexes = {
    @Index(name = "idx_scur_credential", columnList = "credential_id"),
    @Index(name = "idx_scur_user", columnList = "user_id")
})
public class StoredCredentialUserRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credential_id", nullable = false)
    private StoredCredential credential;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AllowedUser user;

    public StoredCredentialUserRecipient() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public StoredCredential getCredential() { return credential; }
    public void setCredential(StoredCredential credential) { this.credential = credential; }
    public AllowedUser getUser() { return user; }
    public void setUser(AllowedUser user) { this.user = user; }
}
