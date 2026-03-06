package com.cyph.repository;

import com.cyph.domain.StoredCredentialUserRecipient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoredCredentialUserRecipientRepository extends JpaRepository<StoredCredentialUserRecipient, Long> {

    List<StoredCredentialUserRecipient> findByUser_Id(Long userId);

    List<StoredCredentialUserRecipient> findByCredential_Id(Long credentialId);

    void deleteByCredential_Id(Long credentialId);
}
