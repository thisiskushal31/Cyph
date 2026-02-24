package com.cyph.repository;

import com.cyph.domain.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.Set;

public interface GroupRepository extends JpaRepository<Group, Long> {

    Optional<Group> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
