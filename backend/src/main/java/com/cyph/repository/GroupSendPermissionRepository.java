package com.cyph.repository;

import com.cyph.domain.GroupSendPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupSendPermissionRepository extends JpaRepository<GroupSendPermission, Long> {

    boolean existsByFromGroupIdAndToGroupId(Long fromGroupId, Long toGroupId);

    Optional<GroupSendPermission> findByFromGroupIdAndToGroupId(Long fromGroupId, Long toGroupId);

    void deleteByFromGroupIdAndToGroupId(Long fromGroupId, Long toGroupId);
}
