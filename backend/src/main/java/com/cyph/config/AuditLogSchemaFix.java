package com.cyph.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Drops the legacy audit_log_event_type_check constraint if present.
 * The constraint was created with an older set of event types and rejects
 * USER_DELETED, USER_CREATED, etc. Dropping it allows all {@link com.cyph.domain.AuditLog.EventType} values.
 */
@Component
public class AuditLogSchemaFix {

    private static final Logger log = LoggerFactory.getLogger(AuditLogSchemaFix.class);

    private final JdbcTemplate jdbcTemplate;

    public AuditLogSchemaFix(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Order(100)
    public void dropLegacyEventTypeConstraint() {
        try {
            jdbcTemplate.execute("ALTER TABLE audit_log DROP CONSTRAINT IF EXISTS audit_log_event_type_check");
            log.debug("Dropped audit_log_event_type_check constraint if present");
        } catch (Exception e) {
            // Ignore: table may not exist yet (H2), or DB may use different DDL (MySQL uses different syntax)
            log.trace("audit_log constraint drop skipped: {}", e.getMessage());
        }
    }
}
