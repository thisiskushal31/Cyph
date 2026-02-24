package com.cyph.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class SendSecretRequest {

    @NotBlank(message = "recipientEmail is required")
    @Email
    private String recipientEmail;

    @NotBlank(message = "message is required")
    private String message;

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
