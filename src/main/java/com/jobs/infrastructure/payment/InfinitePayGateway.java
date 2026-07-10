package com.jobs.infrastructure.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jobs.application.port.PaymentGateway;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

// Checkout por link: criamos um link de pagamento (POST /links) e redirecionamos o usuário pra
// página hospedada pela InfinitePay (PIX/cartão já prontos). A confirmação chega via webhook,
// mas o valor do webhook nunca é confiado sozinho — payment_check confirma o pagamento no servidor
// deles antes de liberar o plano PLUS (evita que uma chamada forjada ao nosso webhook libere plano de graça).
public class InfinitePayGateway implements PaymentGateway {

    private static final String LINKS_URL = "https://api.checkout.infinitepay.io/links";
    private static final String PAYMENT_CHECK_URL = "https://api.checkout.infinitepay.io/payment_check";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String handle;

    public InfinitePayGateway(HttpClient httpClient, ObjectMapper objectMapper, String handle) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.handle = handle;
    }

    @Override
    public String createCheckoutLink(String orderNsu, long amountCents, String description, String redirectUrl,
            String webhookUrl) throws IOException, InterruptedException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("handle", handle);
        root.put("redirect_url", redirectUrl);
        root.put("webhook_url", webhookUrl);
        root.put("order_nsu", orderNsu);
        ArrayNode items = root.putArray("items");
        ObjectNode item = items.addObject();
        item.put("quantity", 1);
        item.put("price", amountCents);
        item.put("description", description);

        HttpResponse<String> response = post(LINKS_URL, objectMapper.writeValueAsString(root));
        if (response.statusCode() != 200) {
            throw new IOException("Falha ao criar link de pagamento na InfinitePay (status "
                    + response.statusCode() + "): " + response.body());
        }

        String url = objectMapper.readTree(response.body()).path("url").asText("");
        if (url.isBlank()) {
            throw new IOException("Resposta da InfinitePay sem 'url': " + response.body());
        }
        return url;
    }

    @Override
    public boolean verifyPayment(String orderNsu, String transactionNsu, String slug)
            throws IOException, InterruptedException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("handle", handle);
        root.put("order_nsu", orderNsu);
        root.put("transaction_nsu", transactionNsu);
        root.put("slug", slug);

        HttpResponse<String> response = post(PAYMENT_CHECK_URL, objectMapper.writeValueAsString(root));
        if (response.statusCode() != 200) {
            return false;
        }

        JsonNode result = objectMapper.readTree(response.body());
        return result.path("success").asBoolean(false) && result.path("paid").asBoolean(false);
    }

    private HttpResponse<String> post(String url, String body) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(15))
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
