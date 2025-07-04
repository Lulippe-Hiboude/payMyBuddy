package com.lulippe.paymybuddy.persistence.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Builder
@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "app_user")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @Column(name = "account", nullable = false, precision = 15, scale = 2)
    private BigDecimal account;

    @ManyToMany
    @JoinTable(
            name = "user_friend",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "friend_id")
    )
    @ToString.Exclude
    @Builder.Default
    private Set<AppUser> friends = new HashSet<>();

    @OneToMany(mappedBy = "sender",cascade = CascadeType.ALL,orphanRemoval = true,fetch = FetchType.EAGER)
    @ToString.Exclude
    @Builder.Default
    private Set<Transaction> sentTransactions = new HashSet<>();

    @OneToMany(mappedBy = "receiver", cascade = CascadeType.ALL,orphanRemoval = true,fetch = FetchType.EAGER)
    @ToString.Exclude
    @Builder.Default
    private Set<Transaction> receivedTransactions = new HashSet<>();

}
