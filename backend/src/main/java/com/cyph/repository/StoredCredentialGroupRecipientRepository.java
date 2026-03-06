package com.cyph.repository;

import com.cyph.domain.StoredCredentialGroupRecipient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoredCredentialGroupRecipientRepository extends JpaRepository<StoredCredentialGroupRecipient, Long> {

    List<StoredCredentialGroupRecipient> findByGroup_Id(Long groupId);

    List<StoredCredentialGroupRecipient> findByCredential_Id(Long credentialId);

    void deleteByCredential_Id(Long credentialId);
}
