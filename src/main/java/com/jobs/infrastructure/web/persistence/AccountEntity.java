package com.jobs.infrastructure.web.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "account")
public class AccountEntity {

    @Id
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    // "FREE" ou "PLUS" — atualizado via webhook de pagamento (InfinitePay) quando confirmado.
    // columnDefinition com default evita erro de NOT NULL ao adicionar essa coluna numa tabela
    // que já tem contas existentes (ficariam NULL sem o default).
    @Column(nullable = false, columnDefinition = "varchar(255) default 'FREE'")
    private String plan = "FREE";

    @Column(name = "last_search_at")
    private Instant lastSearchAt;

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

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public Instant getLastSearchAt() {
        return lastSearchAt;
    }

    public void setLastSearchAt(Instant lastSearchAt) {
        this.lastSearchAt = lastSearchAt;
    }
}
