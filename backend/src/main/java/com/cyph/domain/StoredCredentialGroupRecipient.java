package com.cyph.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

/**
 * Assignment of a SHARED credential to a group (all members can access).
 */
@Entity
@Table(name = "stored_credential_group_recipient", indexes = {
    @Index(name = "idx_scgr_credential", columnList = "credential_id"),
    @Index(name = "idx_scgr_group", columnList = "group_id")
})
public class StoredCredentialGroupRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credential_id", nullable = false)
    private StoredCredential credential;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    public StoredCredentialGroupRecipient() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public StoredCredential getCredential() { return credential; }
    public void setCredential(StoredCredential credential) { this.credential = credential; }
    public Group getGroup() { return group; }
    public void setGroup(Group group) { this.group = group; }
}
