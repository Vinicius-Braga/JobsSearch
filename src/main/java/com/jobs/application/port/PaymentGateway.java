package com.jobs.application.port;

import java.io.IOException;

public interface PaymentGateway {

    String createCheckoutLink(String orderNsu, long amountCents, String description, String redirectUrl,
            String webhookUrl) throws IOException, InterruptedException;

    boolean verifyPayment(String orderNsu, String transactionNsu, String slug) throws IOException, InterruptedException;
}
