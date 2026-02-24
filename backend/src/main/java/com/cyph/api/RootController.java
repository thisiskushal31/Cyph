package com.cyph.api;

import com.cyph.config.CyphProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Redirects browser requests from backend root to the frontend (cyph.site-url).
 * Avoids Whitelabel 404 when opening http://localhost:8080/.
 */
@RestController
public class RootController {

    private final CyphProperties cyphProperties;

    public RootController(CyphProperties cyphProperties) {
        this.cyphProperties = cyphProperties;
    }

    @GetMapping("/")
    public ResponseEntity<Void> root() {
        String siteUrl = cyphProperties.getSiteUrl();
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(siteUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
