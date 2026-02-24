package com.cyph.scheduler;

import com.cyph.service.ExpiredMessageCleanupRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Deletes expired secret messages on a schedule. Idempotent: multiple runs or
 * failed previous runs are safe. Redundancy: (1) This job uses ExpiredMessageCleanupRunner
 * with @Retryable so transient failures are retried. (2) If the job still fails, the next
 * cron run will try again. (3) Viewing an expired message in SecretMessageService also
 * deletes it, so we have two cleanup paths.
 */
@Component
public class ExpiredMessageCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(ExpiredMessageCleanupJob.class);

    private final ExpiredMessageCleanupRunner cleanupRunner;

    public ExpiredMessageCleanupJob(ExpiredMessageCleanupRunner cleanupRunner) {
        this.cleanupRunner = cleanupRunner;
    }

    /** Run every 15 minutes. */
    @Scheduled(cron = "${cyph.cleanup.cron:0 */15 * * * *}")
    public void run() {
        try {
            int deleted = cleanupRunner.run();
            if (deleted > 0) {
                log.info("Expired message cleanup deleted {} message(s).", deleted);
            }
        } catch (Exception e) {
            log.error("Expired message cleanup failed after retries; next run will retry (idempotent).", e);
        }
    }
}
