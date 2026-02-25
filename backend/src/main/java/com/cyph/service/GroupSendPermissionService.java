package com.cyph.service;

import com.cyph.domain.Group;
import com.cyph.domain.GroupSendPermission;
import com.cyph.repository.GroupRepository;
import com.cyph.repository.GroupSendPermissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Manages which groups can send (readable) messages to which other groups.
 */
@Service
public class GroupSendPermissionService {

    private final GroupRepository groupRepository;
    private final GroupSendPermissionRepository permissionRepository;

    public GroupSendPermissionService(GroupRepository groupRepository,
                                     GroupSendPermissionRepository permissionRepository) {
        this.groupRepository = groupRepository;
        this.permissionRepository = permissionRepository;
    }

    /**
     * True if any of the sender's groups is allowed to send to any of the recipient's groups
     * (or they share a group, which is handled by caller).
     */
    public boolean canSendTo(List<String> senderGroupNames, List<String> recipientGroupNames) {
        if (senderGroupNames == null || recipientGroupNames == null || senderGroupNames.isEmpty() || recipientGroupNames.isEmpty()) return false;
        for (String fromName : senderGroupNames) {
            var fromGroup = groupRepository.findByNameIgnoreCase(fromName);
            if (fromGroup.isEmpty()) continue;
            for (String toName : recipientGroupNames) {
                var toGroup = groupRepository.findByNameIgnoreCase(toName);
                if (toGroup.isEmpty()) continue;
                if (permissionRepository.existsByFromGroupIdAndToGroupId(fromGroup.get().getId(), toGroup.get().getId())) {
                    return true;
                }
            }
        }
        return false;
    }

    public List<GroupDto> listGroups() {
        return groupRepository.findAllByOrderByNameAsc().stream()
                .map(g -> new GroupDto(g.getId(), g.getName()))
                .toList();
    }

    @Transactional
    public GroupDto createGroup(String name) {
        String trimmed = name != null ? name.trim() : "";
        if (trimmed.isBlank()) throw new IllegalArgumentException("Group name is required");
        if (groupRepository.existsByNameIgnoreCase(trimmed)) {
            return groupRepository.findByNameIgnoreCase(trimmed)
                    .map(g -> new GroupDto(g.getId(), g.getName()))
                    .orElseThrow();
        }
        Group g = new Group();
        g.setName(trimmed);
        g = groupRepository.save(g);
        return new GroupDto(g.getId(), g.getName());
    }

    @Transactional(readOnly = true)
    public List<GroupPermissionDto> listPermissions() {
        return permissionRepository.findAll().stream()
                .sorted((a, b) -> {
                    int c = a.getFromGroup().getName().compareToIgnoreCase(b.getFromGroup().getName());
                    return c != 0 ? c : a.getToGroup().getName().compareToIgnoreCase(b.getToGroup().getName());
                })
                .map(p -> new GroupPermissionDto(
                        p.getFromGroup().getId(),
                        p.getFromGroup().getName(),
                        p.getToGroup().getId(),
                        p.getToGroup().getName()))
                .toList();
    }

    @Transactional
    public GroupPermissionDto addPermission(String fromGroupName, String toGroupName) {
        var from = groupRepository.findByNameIgnoreCase(fromGroupName != null ? fromGroupName.trim() : "")
                .orElseThrow(() -> new IllegalArgumentException("From group not found: " + fromGroupName));
        var to = groupRepository.findByNameIgnoreCase(toGroupName != null ? toGroupName.trim() : "")
                .orElseThrow(() -> new IllegalArgumentException("To group not found: " + toGroupName));
        if (from.getId().equals(to.getId())) {
            throw new IllegalArgumentException("From and to group cannot be the same");
        }
        if (permissionRepository.existsByFromGroupIdAndToGroupId(from.getId(), to.getId())) {
            return new GroupPermissionDto(from.getId(), from.getName(), to.getId(), to.getName());
        }
        GroupSendPermission p = new GroupSendPermission(from, to);
        p = permissionRepository.save(p);
        return new GroupPermissionDto(p.getFromGroup().getId(), p.getFromGroup().getName(),
                p.getToGroup().getId(), p.getToGroup().getName());
    }

    @Transactional
    public void removePermission(Long fromGroupId, Long toGroupId) {
        permissionRepository.deleteByFromGroupIdAndToGroupId(fromGroupId, toGroupId);
    }

    public record GroupDto(Long id, String name) {}
    public record GroupPermissionDto(Long fromGroupId, String fromGroupName, Long toGroupId, String toGroupName) {}
}
