package com.cyph.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class AddUserRequest {

    @NotBlank
    @Email
    private String email;

    /** Display name (username) for the user. */
    private String username;

    /** Plain password; will be hashed before storing. Optional for invite-only users. */
    private String password;

    /** Group name to assign the user to (created if missing). */
    private String group;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }
}
