package com.lulippe.paymybuddy.persistence.repository;

import com.lulippe.paymybuddy.persistence.entities.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
}
