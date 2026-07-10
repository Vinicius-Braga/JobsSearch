package com.jobs.infrastructure.web.persistence;

import com.jobs.application.port.PaymentStore;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class JpaPaymentStore implements PaymentStore {

    private final PendingPaymentJpaRepository repository;

    public JpaPaymentStore(PendingPaymentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void createPending(String orderNsu, String username) {
        repository.save(new PendingPaymentEntity(orderNsu, username));
    }

    @Override
    public Optional<String> findUsernameByOrderNsu(String orderNsu) {
        return repository.findById(orderNsu).map(PendingPaymentEntity::getUsername);
    }

    @Override
    public void markPaid(String orderNsu, String transactionNsu) {
        repository.findById(orderNsu).ifPresent(payment -> {
            payment.setStatus("PAID");
            payment.setTransactionNsu(transactionNsu);
            repository.save(payment);
        });
    }
}
