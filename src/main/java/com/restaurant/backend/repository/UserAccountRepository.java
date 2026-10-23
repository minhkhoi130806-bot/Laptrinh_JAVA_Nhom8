package com.restaurant.backend.repository;

import com.restaurant.backend.entity.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
    Optional<UserAccount> findByUsername(String username);

    boolean existsByUsername(String username);

    Optional<UserAccount> findByEmployeeId(Long employeeId);

    Optional<UserAccount> findByCustomerId(Long customerId);
}
