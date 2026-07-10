package com.jobs.infrastructure.web.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "pending_payment")
public class PendingPaymentEntity {

    @Id
    private String orderNsu;

    @Column(nullable = false)
    private String username;

    // "PENDING" ou "PAID"
    @Column(nullable = false)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "transaction_nsu")
    private String transactionNsu;

    protected PendingPaymentEntity() {
        // JPA
    }

    public PendingPaymentEntity(String orderNsu, String username) {
        this.orderNsu = orderNsu;
        this.username = username;
    }

    public String getOrderNsu() {
        return orderNsu;
    }

    public String getUsername() {
        return username;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTransactionNsu() {
        return transactionNsu;
    }

    public void setTransactionNsu(String transactionNsu) {
        this.transactionNsu = transactionNsu;
    }
}
