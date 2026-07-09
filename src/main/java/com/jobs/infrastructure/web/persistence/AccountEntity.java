package com.jobs.infrastructure.web.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "account")
public class AccountEntity {

    @Id
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    protected AccountEntity() {
        // JPA
    }

    public AccountEntity(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }
}
