package com.cyph.repository;

import com.cyph.domain.AllowedUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AllowedUserRepository extends JpaRepository<AllowedUser, Long> {

    Optional<AllowedUser> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<AllowedUser> findAllByOrderByCreatedAtDesc();
}
