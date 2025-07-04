package com.lulippe.paymybuddy.persistence.repository;

import com.lulippe.paymybuddy.persistence.entities.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
