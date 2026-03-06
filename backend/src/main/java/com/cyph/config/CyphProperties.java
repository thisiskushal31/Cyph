package com.cyph.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.List;

/**
 * Root configuration for Cyph. All organization-specific and environment-specific
 * values are read from here (file or env). No hardcoded domains or credentials.
 */
@ConfigurationProperties(prefix = "cyph")
@Validated
public class CyphProperties {

    /**
     * Public base URL of the application (e.g. https://cyph.yourcompany.com).
     * Used in email links and redirects.
     */
    @NotBlank(message = "cyph.site-url is required")
    private String siteUrl = "http://localhost:4200";

    private Message message = new Message();
    private Auth auth = new Auth();
    private Mail mail = new Mail();
    private Cleanup cleanup = new Cleanup();

    public String getSiteUrl() {
        return siteUrl;
    }

    public void setSiteUrl(String siteUrl) {
        this.siteUrl = siteUrl;
    }

    public Message getMessage() {
        return message;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public Auth getAuth() {
        return auth;
    }

    public void setAuth(Auth auth) {
        this.auth = auth;
    }

    public Mail getMail() {
        return mail;
    }

    public void setMail(Mail mail) {
        this.mail = mail;
    }

    public Cleanup getCleanup() {
        return cleanup;
    }

    public void setCleanup(Cleanup cleanup) {
        this.cleanup = cleanup;
    }

    public static class Cleanup {
        /** Cron for expired message cleanup. Default: every 15 minutes. */
        private String cron = "0 */15 * * * *";

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }
    }

    public static class Message {
        @Positive
        private int defaultTtlHours = 24;

        public int getDefaultTtlHours() {
            return defaultTtlHours;
        }

        public void setDefaultTtlHours(int defaultTtlHours) {
            this.defaultTtlHours = defaultTtlHours;
        }
    }

    public static class Auth {
        private Sso sso = new Sso();
        /**
         * OAuth2 registration IDs to show on the login page (e.g. ["sso", "google"]).
         * Each must have a corresponding spring.security.oauth2.client.registration.{id} configured.
         */
        private List<String> oauth2RegistrationIds = Collections.emptyList();
        /**
         * @deprecated Use oauth2RegistrationIds. When set, GET /login used to redirect to this single provider.
         */
        @Deprecated
        private String oauth2LoginRegistrationId;
        /**
         * Optional list of allowed email domains (e.g. yourcompany.com).
         * Empty = no domain restriction when not using allowed-user list.
         */
        private List<String> allowedDomains = Collections.emptyList();
        /**
         * When true, only users present in allowed_user table (or in admin list) can sign in.
         * When false, allowedDomains (if set) or any user can sign in depending on provider.
         */
        private boolean requireAllowedUserList = false;
        /**
         * Emails that are always treated as admins (can access admin panel). Used for bootstrap and local testing.
         */
        private List<String> adminEmails = Collections.emptyList();
        /**
         * Form-based login for admin account (username + password). When enabled, login page shows "Login to admin account".
         */
        private FormLogin formLogin = new FormLogin();

        public FormLogin getFormLogin() {
            return formLogin;
        }

        public void setFormLogin(FormLogin formLogin) {
            this.formLogin = formLogin;
        }

        public Sso getSso() {
            return sso;
        }

        public void setSso(Sso sso) {
            this.sso = sso;
        }

        public List<String> getOauth2RegistrationIds() {
            return oauth2RegistrationIds != null ? oauth2RegistrationIds : Collections.emptyList();
        }

        public void setOauth2RegistrationIds(List<String> oauth2RegistrationIds) {
            this.oauth2RegistrationIds = oauth2RegistrationIds != null ? oauth2RegistrationIds : Collections.emptyList();
        }

        public String getOauth2LoginRegistrationId() {
            return oauth2LoginRegistrationId;
        }

        public void setOauth2LoginRegistrationId(String oauth2LoginRegistrationId) {
            this.oauth2LoginRegistrationId = oauth2LoginRegistrationId;
        }

        public List<String> getAllowedDomains() {
            return allowedDomains;
        }

        public void setAllowedDomains(List<String> allowedDomains) {
            this.allowedDomains = allowedDomains != null ? allowedDomains : Collections.emptyList();
        }

        public boolean isRequireAllowedUserList() {
            return requireAllowedUserList;
        }

        public void setRequireAllowedUserList(boolean requireAllowedUserList) {
            this.requireAllowedUserList = requireAllowedUserList;
        }

        public List<String> getAdminEmails() {
            return adminEmails;
        }

        public void setAdminEmails(List<String> adminEmails) {
            this.adminEmails = adminEmails != null ? adminEmails : Collections.emptyList();
        }

        /** JWT for extension auth. When set, extension can log in and get a token. */
        private ExtensionJwt extensionJwt = new ExtensionJwt();

        public ExtensionJwt getExtensionJwt() {
            return extensionJwt;
        }

        public void setExtensionJwt(ExtensionJwt extensionJwt) {
            this.extensionJwt = extensionJwt;
        }

        public static class ExtensionJwt {
            /** Secret for signing JWTs (min 256 bits / 32 bytes for HS256). Set via env in production. */
            private String secret = "change-me-extension-jwt-secret-min-32-bytes-long-for-hs256";
            /** Token validity in minutes. */
            private int expirationMinutes = 60 * 24; // 24 hours

            public String getSecret() { return secret; }
            public void setSecret(String secret) { this.secret = secret; }
            public int getExpirationMinutes() { return expirationMinutes; }
            public void setExpirationMinutes(int expirationMinutes) { this.expirationMinutes = expirationMinutes; }
        }

        public static class FormLogin {
            private boolean enabled = false;
            /** Admin username (e.g. admin@company.com). Should be in admin-emails so user has admin access. */
            private String username;
            /** Admin password (plain; set via env in production). */
            private String password;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
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
        }

        public static class Sso {
            private boolean enabled = false;
            private String issuerUri;
            private String clientId;
            private String clientSecret;
            private String scope = "openid email profile";
            /** JWT claim name that contains list of group names (e.g. "groups" or "realm_access.roles"). Used to sync user groups on login. */
            private String groupsClaim = "groups";

            public String getGroupsClaim() {
                return groupsClaim;
            }

            public void setGroupsClaim(String groupsClaim) {
                this.groupsClaim = groupsClaim;
            }

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public String getIssuerUri() {
                return issuerUri;
            }

            public void setIssuerUri(String issuerUri) {
                this.issuerUri = issuerUri;
            }

            public String getClientId() {
                return clientId;
            }

            public void setClientId(String clientId) {
                this.clientId = clientId;
            }

            public String getClientSecret() {
                return clientSecret;
            }

            public void setClientSecret(String clientSecret) {
                this.clientSecret = clientSecret;
            }

            public String getScope() {
                return scope;
            }

            public void setScope(String scope) {
                this.scope = scope;
            }
        }
    }

    public static class Mail {
        private String host;
        private int port = 587;
        private String username;
        private String password;
        @NotBlank(message = "cyph.mail.from is required when sending messages")
        private String from = "noreply@localhost";

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
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

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }
    }
}
