package com.cyph.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request body for removing a user (POST /api/v1/admin/users/remove).
 * Email in body avoids path-encoding and can improve session/cookie behavior vs DELETE.
 */
public class RemoveUserRequest {

    @NotBlank(message = "email is required")
    @Email
    private String email;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
