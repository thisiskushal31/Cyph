package com.cyph.api;

import com.cyph.config.CyphProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * GET /login: redirects to the frontend login page (siteUrl/login) so the user
 * can choose SSO, Google, or admin username/password. Never 404s.
 */
@RestController
public class LoginController {

    private final CyphProperties cyphProperties;

    public LoginController(CyphProperties cyphProperties) {
        this.cyphProperties = cyphProperties;
    }

    @GetMapping("/login")
    public ResponseEntity<Void> login(
            @RequestParam(value = "redirect", required = false) String redirect,
            @RequestParam(value = "error", required = false) String error) {
        String siteUrl = cyphProperties.getSiteUrl();
        StringBuilder target = new StringBuilder(siteUrl + "/login");
        String sep = "?";
        if (redirect != null && !redirect.isBlank()) {
            target.append(sep).append("redirect=").append(URLEncoder.encode(redirect, StandardCharsets.UTF_8));
            sep = "&";
        }
        if (error != null && !error.isBlank()) {
            target.append(sep).append("error=").append(URLEncoder.encode(error, StandardCharsets.UTF_8));
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(target.toString()));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
