package com.cyph.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Organization group (e.g. tech, business). Users belong to groups via SSO claim or admin assignment.
 * Same-group users can read messages; cross-group messages are locked.
 */
@Entity
@Table(name = "app_group", indexes = {
    @Index(name = "idx_app_group_name", columnList = "name", unique = true)
})
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 128)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToMany(mappedBy = "groups")
    private Set<AllowedUser> users = new HashSet<>();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Group() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Set<AllowedUser> getUsers() {
        return users;
    }

    public void setUsers(Set<AllowedUser> users) {
        this.users = users;
    }
}
