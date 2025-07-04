package com.lulippe.paymybuddy.persistence.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaction_id")
    private Long transactionId;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "sender_id")
    private AppUser sender;

    @ManyToOne(fetch = FetchType.LAZY,optional = false)
    @JoinColumn(name = "receiver_id")
    private AppUser receiver;
}
