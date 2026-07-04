package com.enterprise.issuetracker.repository;

import com.enterprise.issuetracker.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StatusRepository extends JpaRepository<Status, Long> {
    Optional<Status> findByName(String name);
    Boolean existsByName(String name);
}
