package com.cyph.service;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * Wrapper so that deleteExpired is retried (transient failures). Called by the scheduled job;
 * must be in a separate bean for @Retryable to apply.
 */
@Service
public class ExpiredMessageCleanupRunner {

    private final SecretMessageService secretMessageService;

    public ExpiredMessageCleanupRunner(SecretMessageService secretMessageService) {
        this.secretMessageService = secretMessageService;
    }

    @Retryable(
            retryFor = { Exception.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public int run() {
        return secretMessageService.deleteExpired();
    }
}
