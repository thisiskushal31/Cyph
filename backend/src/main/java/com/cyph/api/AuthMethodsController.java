package com.cyph.api;

import com.cyph.config.CyphProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public API for the login page: which login methods are available (SSO, Google, admin form).
 */
@RestController
@RequestMapping(ApiV1.BASE + "/public")
public class AuthMethodsController {

    private final CyphProperties cyphProperties;

    public AuthMethodsController(CyphProperties cyphProperties) {
        this.cyphProperties = cyphProperties;
    }

    @GetMapping("/auth-methods")
    public ResponseEntity<Map<String, Object>> authMethods() {
        return ResponseEntity.ok(Map.of(
                "oauth2RegistrationIds", cyphProperties.getAuth().getOauth2RegistrationIds(),
                "formLogin", cyphProperties.getAuth().getFormLogin().isEnabled()
        ));
    }
}
