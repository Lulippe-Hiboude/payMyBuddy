package com.lulippe.paymybuddy.persistence.repository;

import com.lulippe.paymybuddy.persistence.entities.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    boolean existsByUsernameOrEmail(String username, String email);

    Optional<AppUser> findByUsername(String username);
}