package com.cyph.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for viewing a secret message (POST /api/v1/view).
 * Using POST with body ensures session cookies are sent reliably.
 */
public class ViewSecretRequest {

    @NotBlank(message = "accessToken is required")
    private String accessToken;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
}
