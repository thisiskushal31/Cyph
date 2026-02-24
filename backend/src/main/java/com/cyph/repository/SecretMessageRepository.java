package com.cyph.repository;

import com.cyph.domain.SecretMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface SecretMessageRepository extends JpaRepository<SecretMessage, Long> {

    Optional<SecretMessage> findByAccessToken(String accessToken);

    List<SecretMessage> findAllByExpiresAtBefore(Instant before);

    @Modifying
    @Query("DELETE FROM SecretMessage s WHERE s.expiresAt < :before")
    int deleteExpiredBefore(@Param("before") Instant before);

    boolean existsByAccessToken(String accessToken);
}
