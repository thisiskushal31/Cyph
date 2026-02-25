package com.cyph.domain;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * Allows messages from one group to be read by another (cross-group).
 * If no permission exists, cross-group messages are locked.
 */
@Entity
@Table(name = "group_send_permission", indexes = {
    @Index(name = "idx_group_send_perm_from_to", columnList = "from_group_id, to_group_id", unique = true)
})
public class GroupSendPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_group_id", nullable = false)
    private Group fromGroup;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_group_id", nullable = false)
    private Group toGroup;

    public GroupSendPermission() {
    }

    public GroupSendPermission(Group fromGroup, Group toGroup) {
        this.fromGroup = fromGroup;
        this.toGroup = toGroup;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Group getFromGroup() {
        return fromGroup;
    }

    public void setFromGroup(Group fromGroup) {
        this.fromGroup = fromGroup;
    }

    public Group getToGroup() {
        return toGroup;
    }

    public void setToGroup(Group toGroup) {
        this.toGroup = toGroup;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GroupSendPermission that = (GroupSendPermission) o;
        return Objects.equals(fromGroup != null ? fromGroup.getId() : null, that.fromGroup != null ? that.fromGroup.getId() : null)
                && Objects.equals(toGroup != null ? toGroup.getId() : null, that.toGroup != null ? that.toGroup.getId() : null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fromGroup != null ? fromGroup.getId() : null, toGroup != null ? toGroup.getId() : null);
    }
}
