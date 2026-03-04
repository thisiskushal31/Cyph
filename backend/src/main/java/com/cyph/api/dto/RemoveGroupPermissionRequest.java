package com.cyph.api.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request body for removing a group send permission (POST /api/v1/admin/group-permissions/remove).
 */
public class RemoveGroupPermissionRequest {

    @NotNull(message = "fromGroupId is required")
    private Long fromGroupId;

    @NotNull(message = "toGroupId is required")
    private Long toGroupId;

    public Long getFromGroupId() {
        return fromGroupId;
    }

    public void setFromGroupId(Long fromGroupId) {
        this.fromGroupId = fromGroupId;
    }

    public Long getToGroupId() {
        return toGroupId;
    }

    public void setToGroupId(Long toGroupId) {
        this.toGroupId = toGroupId;
    }
}
