package com.cyph.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for setting a user's admin flag (POST /api/v1/admin/users/set-admin).
 */
public class SetAdminRequest {

    @NotBlank(message = "email is required")
    @Email
    private String email;

    @NotNull(message = "admin flag is required")
    private Boolean admin;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getAdmin() {
        return admin;
    }

    public void setAdmin(Boolean admin) {
        this.admin = admin;
    }
}
