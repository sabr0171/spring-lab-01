package com.example.txlab.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "owner_name", nullable = false, length = 120)
    private String ownerName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    // Added by V2__add_status.sql; ddl-auto=validate fails at startup if that migration is missing
    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    // Optimistic locking: Hibernate adds "AND version = ?" to every UPDATE and increments it
    @Version
    private Long version;

    protected Account() {
        // required by JPA
    }

    public Account(String ownerName, BigDecimal balance) {
        this.ownerName = ownerName;
        this.balance = balance;
    }

    public void debit(BigDecimal amount) {
        if (amount.signum() <= 0) throw new IllegalArgumentException("Amount must be positive");
        if (balance.compareTo(amount) < 0) throw new IllegalStateException("Insufficient funds");
        balance = balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        if (amount.signum() <= 0) throw new IllegalArgumentException("Amount must be positive");
        balance = balance.add(amount);
    }

    public Long getId() {
        return id;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public String getStatus() {
        return status;
    }

    public Long getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "Account{id=" + id + ", owner=" + ownerName + ", balance=" + balance + ", version=" + version + "}";
    }
}
