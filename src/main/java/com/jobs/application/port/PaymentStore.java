package com.jobs.application.port;

import java.util.Optional;

public interface PaymentStore {

    void createPending(String orderNsu, String username);

    Optional<String> findUsernameByOrderNsu(String orderNsu);

    void markPaid(String orderNsu, String transactionNsu);
}
