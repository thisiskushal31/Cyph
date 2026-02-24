package com.cyph.security;

import com.cyph.config.CyphProperties;
import com.cyph.domain.AllowedUser;
import com.cyph.service.AllowedUserService;
import com.cyph.service.AuditService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * After successful OAuth2/OIDC login: ensure user is allowed, upsert in allowed_user table,
 * then redirect. If not allowed, redirect to login with error.
 */
@Component
public class CyphOAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final AllowedUserService allowedUserService;
    private final AuditService auditService;
    private final CyphProperties cyphProperties;

    public CyphOAuth2LoginSuccessHandler(AllowedUserService allowedUserService, AuditService auditService, CyphProperties cyphProperties) {
        this.allowedUserService = allowedUserService;
        this.auditService = auditService;
        this.cyphProperties = cyphProperties;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {
        String email = emailFrom(authentication);
        if (email == null || email.isBlank()) {
            redirectToLoginWithError(response, "no_email");
            return;
        }
        if (!allowedUserService.isAllowedToSignIn(email)) {
            redirectToLoginWithError(response, "not_allowed");
            return;
        }
        String externalId = externalIdFrom(authentication);
        String registrationId = authentication instanceof OAuth2AuthenticationToken token
                ? token.getAuthorizedClientRegistrationId()
                : "unknown";
        AllowedUser.Source source = cyphProperties.getAuth().getSso().isEnabled()
                && "sso".equalsIgnoreCase(registrationId)
                ? AllowedUser.Source.SSO
                : AllowedUser.Source.ADMIN_ADDED;
        List<String> groupNames = extractGroupNamesFromToken(authentication);
        allowedUserService.upsertFromLogin(email, externalId, source, groupNames);
        auditService.logLogin();
        super.onAuthenticationSuccess(request, response, authentication);
    }

    /** Extract group names from IdP token (e.g. "groups" or "realm_access.roles"). No PII. */
    private List<String> extractGroupNamesFromToken(Authentication authentication) {
        String claimName = cyphProperties.getAuth().getSso().getGroupsClaim();
        if (claimName == null || claimName.isBlank()) return Collections.emptyList();
        Object claim = null;
        if (authentication.getPrincipal() instanceof OidcUser oidc) {
            claim = oidc.getClaim(claimName);
            if (claim == null && claimName.contains(".")) {
                String[] path = claimName.split("\\.");
                Object current = oidc.getClaims();
                for (String p : path) {
                    if (current instanceof java.util.Map<?, ?> map) {
                        current = map.get(p);
                    } else break;
                }
                claim = current;
            }
        } else if (authentication.getPrincipal() instanceof OAuth2User oauth2) {
            claim = oauth2.getAttribute(claimName);
        }
        if (claim instanceof List<?> list) {
            return list.stream()
                    .filter(o -> o != null && !o.toString().isBlank())
                    .map(o -> o.toString().trim())
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private void redirectToLoginWithError(HttpServletResponse response, String error) throws IOException {
        String location = "/login?error=" + error;
        response.sendRedirect(location);
    }

    private static String emailFrom(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OidcUser oidc) {
            return oidc.getEmail();
        }
        if (authentication.getPrincipal() instanceof OAuth2User oauth2) {
            return oauth2.getAttribute("email");
        }
        return null;
    }

    private static String externalIdFrom(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OidcUser oidc && oidc.getSubject() != null) {
            return oidc.getSubject();
        }
        if (authentication.getPrincipal() instanceof OAuth2User oauth2) {
            Object sub = oauth2.getAttribute("sub");
            return sub != null ? sub.toString() : null;
        }
        return null;
    }
}
