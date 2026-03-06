package com.cyph.repository;

import com.cyph.domain.StoredCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoredCredentialRepository extends JpaRepository<StoredCredential, Long> {

    List<StoredCredential> findByOwnerUserIdOrderByLabelAsc(Long ownerUserId);

    List<StoredCredential> findByTypeOrderByCreatedAtDesc(StoredCredential.Type type);
}
